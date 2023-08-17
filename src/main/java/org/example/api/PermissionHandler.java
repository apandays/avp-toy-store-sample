package org.example.api;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.verifiedpermissions.AmazonVerifiedPermissions;
import com.amazonaws.services.verifiedpermissions.AmazonVerifiedPermissionsClientBuilder;
import com.amazonaws.services.verifiedpermissions.model.*;
import org.example.config.RoleCedarTemplates;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PermissionHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {


    public static final String POLICY_STORE_ID = System.getenv("policyStoreId");

    public static final String USER_POOL_ID = System.getenv("userPoolId");


    private final AmazonVerifiedPermissions amazonVerifiedPermissionsClient = AmazonVerifiedPermissionsClientBuilder.
            defaultClient();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {

        System.out.println(event);
        String employeeId = event.getPathParameters().get("employee-id");
        String sub = getSubFromUsername(employeeId);
        String storeId = event.getPathParameters().get("store-id");

        CreatePolicyRequest createPolicyRequest= new CreatePolicyRequest().
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
        System.out.println("Filters "+policyFilter.toString());
        System.out.println("Got these policies "+ policies.toString());
        if(policies.size()>0) {
            APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
            System.out.println("The policy already exists for principal " + employeeId + " and store " + storeId);
            response.setStatusCode(200);
            response.setBody(policies.get(0).getPolicyId());
            return response;
        }
        System.out.println("Creating policy for principal " + employeeId +" with principal id " + USER_POOL_ID + "|" + sub + " and store " + storeId);

        createPolicyRequest.setDefinition(
                new PolicyDefinition().
                        withTemplateLinked(
                                new TemplateLinkedPolicyDefinition().
                                        withPolicyTemplateId(RoleCedarTemplates.getCedarTemplateIdFromHttpPath(event.getRouteKey())).withPrincipal(user).withResource(store)));


        CreatePolicyResult createPolicyResult = amazonVerifiedPermissionsClient.createPolicy(createPolicyRequest);

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(200);
        response.setBody(createPolicyResult.getPolicyId());
        return response;
    }

    private static String getSubFromUsername(String employeeId) {
        AdminGetUserRequest adminGetUserRequest = new AdminGetUserRequest();
        adminGetUserRequest.setUsername(employeeId);
        adminGetUserRequest.setUserPoolId(USER_POOL_ID);
        List<AttributeType> attributes = AWSCognitoIdentityProviderClientBuilder.defaultClient().adminGetUser(adminGetUserRequest).getUserAttributes();
        String sub = attributes.stream()
                .filter(attribute -> attribute.getName().equals("sub"))
                .collect(Collectors.toList()).stream().findFirst().orElseThrow(() -> {
                        throw new InternalError("No sub attribute present in JWT token");
                }).getValue();
        return sub;
    }
}
