package org.example.config;

import com.amazonaws.util.StringUtils;

import java.util.Map;

public class HttpPathToCedarActionMap {

    public static Map<String, String> httpPathToCedarActionMap = Map.of(
            StringUtils.lowerCase("GET /store/{store-id}/orders"), "ListOrders",
            StringUtils.lowerCase("GET /store/{store-id}/order/{order-Id}"), "GetOrder",
            StringUtils.lowerCase("GET /store/{store-id}/order/{order-Id}/label"), "GetOrderLabel",
            StringUtils.lowerCase("GET /store/{store-id}/order/{order-Id}/receipt"), "GetOrderReceipt",
            StringUtils.lowerCase("GET /store/{store-id}/order/{order-Id}/box_size"), "GetOrderBoxSize",
            StringUtils.lowerCase("DELETE /store/{store-id}/order/{order-Id}"), "DeleteOrder",
            StringUtils.lowerCase("PUT /store/{store-id}/pack_associate/{employee-id}"), "AddPackAssociate",
            StringUtils.lowerCase("PUT /store/{store-id}/store_manager/{employee-id}"), "AddStoreManager",
            StringUtils.lowerCase("GET /store/{store-id}/pack_associate"), "ListPackAssociates",
            StringUtils.lowerCase("GET /store/{store-id}/store_manager"), "ListStoreManagers"
    );

    public static String getCedarAction(String httpRouteKey) {
        String cedarAction = httpPathToCedarActionMap.get(StringUtils.lowerCase(httpRouteKey));
        System.out.println("Action for [" + httpRouteKey +"] is "+cedarAction);
       if (cedarAction == null)
            throw new RuntimeException("No CedarAction found for [" + httpRouteKey +"]");
        return cedarAction;
    }

}
