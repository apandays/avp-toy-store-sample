package org.example.api;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.verifiedpermissions.AmazonVerifiedPermissions;
import com.amazonaws.services.verifiedpermissions.AmazonVerifiedPermissionsClientBuilder;
import com.amazonaws.services.verifiedpermissions.model.*;
import com.google.gson.Gson;
import org.example.config.RoleCedarTemplates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionHandler {


    public static final String POLICY_STORE_ID = System.getenv("policyStoreId");

    public static final String USER_POOL_ID = System.getenv("userPoolId");
    public final AWSCognitoIdentityProvider awsCognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder.defaultClient();

    private final AmazonVerifiedPermissions amazonVerifiedPermissionsClient = AmazonVerifiedPermissionsClientBuilder.
            defaultClient();


    public APIGatewayV2HTTPResponse listUsers(APIGatewayV2HTTPEvent event, Context context) {

        String storeId = event.getPathParameters().get("store-id");

        String templateForRole = RoleCedarTemplates.getCedarTemplateIdFromHttpPath(event.getRouteKey());

        EntityIdentifier store = new EntityIdentifier().
                withEntityId(storeId).
                withEntityType("avp::sample::toy::store::Store");

        List<PolicyItem> policies = getPoliciesForRole(templateForRole, store);
        List<String> users = getUsersFromPolicies(policies);

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(users));
        return response;
    }

    private List<PolicyItem> getPoliciesForRole(String templateForRole, EntityIdentifier store) {
        List<PolicyItem> policies = amazonVerifiedPermissionsClient.listPolicies(
                new ListPoliciesRequest().
                        withPolicyStoreId(POLICY_STORE_ID).
                        withFilter(new PolicyFilter().
                                withPolicyTemplateId(templateForRole).
                                withPolicyType(PolicyType.TEMPLATE_LINKED).
                                withResource(new EntityReference().
                                        withIdentifier(store))
                        )).getPolicies();
        return policies;
    }

    private List<String> getUsersFromPolicies(List<PolicyItem> policies) {
        List<String> principals = new ArrayList<>();
        policies.forEach((policyItem -> {
            String principal = policyItem.getPrincipal().getEntityId();
            String sub = principal.split("\\|")[1];
            System.out.println("Trying to get username for sub " + sub + " with filter " + "sub=\"" + sub + "\"");
            List<UserType> users = awsCognitoIdentityProvider.listUsers(new ListUsersRequest().
                    withUserPoolId(USER_POOL_ID).withFilter("sub=\"" + sub + "\"")
            ).getUsers();
            if (users.size() == 0) {
                throw new InternalError("No user found with sub " + sub);
            }
            String username = users.get(0).getUsername();
            System.out.println("Got username " + username + "for sub " + sub);
            principals.add(username);
        }));
        return principals;
    }

    public APIGatewayV2HTTPResponse grantAccess(APIGatewayV2HTTPEvent event, Context context) {

        System.out.println(event);
        String employeeId = event.getPathParameters().get("employee-id");
        String sub = getSubFromUsername(employeeId);
        String storeId = event.getPathParameters().get("store-id");

        CreatePolicyRequest createPolicyRequest = new CreatePolicyRequest().
                withPolicyStoreId(POLICY_STORE_ID);
        EntityIdentifier user = new EntityIdentifier().
                withEntityId(USER_POOL_ID + "|" + sub).
                withEntityType("avp::sample::toy::store::User");
        EntityIdentifier store = new EntityIdentifier().
                withEntityId(storeId).
                withEntityType("avp::sample::toy::store::Store");

        ListPoliciesRequest listPolicyRequest = new ListPoliciesRequest();
        listPolicyRequest.setPolicyStoreId(POLICY_STORE_ID);
        PolicyFilter policyFilter = new PolicyFilter().withPolicyType(PolicyType.TEMPLATE_LINKED).
                withPolicyTemplateId(RoleCedarTemplates.getCedarTemplateIdFromHttpPath(event.getRouteKey())).
                withPrincipal(new EntityReference().withIdentifier(user)).withResource(new EntityReference().withIdentifier(store));
        listPolicyRequest.setFilter(policyFilter);

        List<PolicyItem> policies = amazonVerifiedPermissionsClient.listPolicies(new ListPoliciesRequest().withPolicyStoreId(POLICY_STORE_ID).withFilter(policyFilter)).getPolicies();
        System.out.println("Filters " + policyFilter.toString());
        System.out.println("Got these policies " + policies.toString());
        if (policies.size() > 0) {
            APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
            System.out.println("The policy already exists for principal " + employeeId + " and store " + storeId);
            response.setStatusCode(200);
            response.setBody("{\"policy-id\":\""+policies.get(0).getPolicyId()+"\"}");
            return response;
        }
        System.out.println("Creating policy for principal " + employeeId + " with principal id " + USER_POOL_ID + "|" + sub + " and store " + storeId);

        createPolicyRequest.setDefinition(
                new PolicyDefinition().
                        withTemplateLinked(
                                new TemplateLinkedPolicyDefinition().
                                        withPolicyTemplateId(RoleCedarTemplates.getCedarTemplateIdFromHttpPath(event.getRouteKey())).withPrincipal(user).withResource(store)));


        CreatePolicyResult createPolicyResult = amazonVerifiedPermissionsClient.createPolicy(createPolicyRequest);

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(200);
        response.setBody("{\"policy-id\":\""+createPolicyResult.getPolicyId()+"\"}");
        return response;
    }

    private String getSubFromUsername(String employeeId) {
        AdminGetUserRequest adminGetUserRequest = new AdminGetUserRequest();
        adminGetUserRequest.setUsername(employeeId);
        adminGetUserRequest.setUserPoolId(USER_POOL_ID);
        List<AttributeType> attributes = awsCognitoIdentityProvider.adminGetUser(adminGetUserRequest).getUserAttributes();
        String sub = attributes.stream()
                .filter(attribute -> attribute.getName().equals("sub"))
                .collect(Collectors.toList()).stream().findFirst().orElseThrow(() -> {
                    throw new InternalError("No sub attribute present in JWT token");
                }).getValue();
        return sub;
    }
}
