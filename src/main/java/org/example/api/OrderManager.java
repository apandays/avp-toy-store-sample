package org.example.api;

import java.util.Arrays;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.simple.JSONObject;


public class OrderManager implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        System.out.println("event: " + event.toString());
        JSONObject response = new JSONObject();
        try {
            if (event.getRouteKey().toLowerCase().equals("GET /store/{store-id}/order/{order-Id}") ||
                    event.getRouteKey().toLowerCase().equals("GET /store/{store-id}/orders")) {
                //Custom environment variables created in JavaCdkStack
                JSONObject envObject = new JSONObject();
                String orderId = event.getPathParameters().get("order-id");
                response.put("orderDescription", "this is a description for order " + orderId);

                return ok(response);
            } else {
                //Custom environment variables created in JavaCdkStack
                JSONObject envObject = new JSONObject();
                String orderId = event.getPathParameters().get("order-id");
                response.put("orderDescription", "Getting back artifact for order  " + orderId);
                return ok(response);
            }
        } catch (Exception exc) {
            return error(response, exc);
        }
    }

    private APIGatewayV2HTTPResponse ok(JSONObject response) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withBody(response.toJSONString())
                .withIsBase64Encoded(false).build();
    }

    private APIGatewayV2HTTPResponse error(JSONObject response, Exception exc) {
        String exceptionString = String.format("error: %s: %s", exc.getMessage(), Arrays.toString(exc.getStackTrace()));
        response.put("Exception", exceptionString);
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(500)
                .withBody(response.toJSONString())
                .withIsBase64Encoded(false).build();
    }


}


