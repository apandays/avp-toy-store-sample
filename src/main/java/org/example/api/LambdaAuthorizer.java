package org.example.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import com.amazonaws.services.verifiedpermissions.AmazonVerifiedPermissionsClientBuilder;
import com.amazonaws.services.verifiedpermissions.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.tuple.Pair;
import org.example.authorizerpolicies.AuthPolicy;
import org.example.config.EntityTypesConstants;
import org.example.config.HttpPathToCedarActionMap;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.resourceEntityBuilders.ResourceEntityBuilderFactory;


public class LambdaAuthorizer implements RequestHandler<APIGatewayV2HTTPEvent, AuthPolicy> {
    // All orders have this entity as the parent. It helps identity entities of type order and give permissions to all orders in the policy head
    // this will no longer be required when Cedar support the "is" operator

    private final ResourceEntityBuilderFactory resourceEntityBuilderFactory = new ResourceEntityBuilderFactory();
    private static final String policyStoreId = System.getenv("policyStoreId");
    public static final String AUTHORIZATION_HEADER_NAME = "authorization";

    private static String getSub(APIGatewayV2HTTPEvent event) throws IOException {
        String jwt = event.getHeaders().get(AUTHORIZATION_HEADER_NAME);
        String claimsBase64 = jwt.substring(jwt.indexOf('.') + 1, jwt.lastIndexOf('.'));

        byte[] claimsByte = Base64.getDecoder().decode(claimsBase64);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> claimsMap = mapper.readValue(claimsByte,
            new TypeReference<Map<String, Object>>() {
        });
        return (String) claimsMap.get("sub");

    }

    @Override
    public AuthPolicy handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            System.out.println("request" + event);

            String sub = getSub(event);
            APIGatewayV2HTTPEvent.RequestContext requestContext = event.getRequestContext();
            Pair<EntityIdentifier, EntitiesDefinition> resourceEntitiesPair =
                    resourceEntityBuilderFactory.getResourceEntityBuilder(
                            event.getRouteKey()).getResourceEntities(event);

            ActionIdentifier actionIdentifier = getActionIdentifier(event);

            IsAuthorizedWithTokenRequest isAuthorizedWithTokenRequest = new IsAuthorizedWithTokenRequest().
                    withIdentityToken(event.getHeaders().get(AUTHORIZATION_HEADER_NAME)).
                    withResource(resourceEntitiesPair.getLeft()).
                    withEntities(resourceEntitiesPair.getRight()).
                    withAction(actionIdentifier).
                    withPolicyStoreId(policyStoreId);

            System.out.println(isAuthorizedWithTokenRequest);
            IsAuthorizedWithTokenResult authorizationResult = AmazonVerifiedPermissionsClientBuilder.
                    defaultClient().isAuthorizedWithToken(isAuthorizedWithTokenRequest);
            System.out.println(authorizationResult);

            return buildAuthorizerResponse(sub, requestContext, authorizationResult.getDecision().toLowerCase());
        } catch (Exception e) {
            System.out.println(e.getMessage() + " " + e.getCause());
            return buildAuthorizerResponse(null, event.getRequestContext(), "Deny");
        }
    }

    private ActionIdentifier getActionIdentifier(APIGatewayV2HTTPEvent event) {
        return new ActionIdentifier().
                withActionType(EntityTypesConstants.ACTION_ENTITY_TYPE).
                withActionId(HttpPathToCedarActionMap.getCedarAction(event.getRouteKey()));
    }



    private AuthPolicy buildAuthorizerResponse(String sub, APIGatewayV2HTTPEvent.RequestContext requestContext, String decision) {
        String accountId = requestContext.getAccountId();
        String pathWithoutStage = getPathWithoutStage(requestContext);
        /*
            The IAM allow policy needs 5 inputs
            1. Principal which in my case is the user sub from the JWT token
            2. AccountId: AWS account id
            3. ApiId: API id : This is the key for the specific API
            4. HTTP Method - GET
            5. Resource - /store/store-1/order/order-1

            Resource  GET /store/store-1/order/order-1/label
                      PUT /store/store-1/order/order-1/pack
         */
        if(decision.equalsIgnoreCase("allow")) {
            return new AuthPolicy(sub, AuthPolicy.PolicyDocument.getAllowOnePolicy(
                    System.getenv("AWS_REGION"), accountId,
                    requestContext.getApiId(), requestContext.getStage(),
                    Enum.valueOf(AuthPolicy.HttpMethod.class, requestContext.getHttp().getMethod()),
                    pathWithoutStage));
        } else {
            return new AuthPolicy(sub, AuthPolicy.PolicyDocument.getDenyOnePolicy(
                    System.getenv("AWS_REGION"), accountId,
                    requestContext.getApiId(), requestContext.getStage(),
                    Enum.valueOf(AuthPolicy.HttpMethod.class, requestContext.getHttp().getMethod()),
                    pathWithoutStage));
        }
    }

    /*
            This is to remove the stage from the path. The HTTP path is '/prod/store/store-1/order/order-1'
            this logic removes the stage from the path and returns '/store/store-1/order/order-1'
         */
    private static String getPathWithoutStage(APIGatewayV2HTTPEvent.RequestContext requestContext) {
        int index = requestContext.getHttp().getPath().indexOf('/',2);
        int pathLength = requestContext.getHttp().getPath().length();
        String pathWithoutStage = requestContext.getHttp().getPath().substring(index+1, pathLength);
        return pathWithoutStage;
    }
}
