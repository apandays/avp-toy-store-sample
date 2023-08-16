package org.example.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.verifiedpermissions.AmazonVerifiedPermissionsClientBuilder;
import com.amazonaws.services.verifiedpermissions.model.*;
import org.example.config.RoleCedarTemplates;

import java.util.ArrayList;

public class PermissionHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {


    public static final String POLICY_STORE_ID = System.getenv("policyStoreId");

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {

        String employeeId = event.getPathParameters().get("employee-id");
        String storeId = event.getPathParameters().get("store-id");

        CreatePolicyRequest createPolicyRequest= new CreatePolicyRequest().
                withPolicyStoreId(POLICY_STORE_ID);
        EntityIdentifier user = new EntityIdentifier().
                withEntityId(employeeId).
                withEntityType("avp::sample::toy::store::User");
        EntityIdentifier store = new EntityIdentifier().
                withEntityId(storeId).
                withEntityType("avp::sample::toy::store::Store");

        createPolicyRequest.setDefinition(
                new PolicyDefinition().
                        withTemplateLinked(
                                new TemplateLinkedPolicyDefinition().
                                        withPolicyTemplateId(RoleCedarTemplates.getCedarTemplateIdFromHttpPath(event.getRouteKey())).withPrincipal(user).withResource(store)));

        CreatePolicyResult createPolicyResult = AmazonVerifiedPermissionsClientBuilder.
                defaultClient().
                createPolicy(createPolicyRequest);

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(200);
        response.setBody(createPolicyResult.getPolicyId());
        return response;
    }
}
