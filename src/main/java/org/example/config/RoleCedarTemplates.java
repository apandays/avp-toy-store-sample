package org.example.config;

public class RoleCedarTemplates {
    public static final String STORE_MANAGER_ROLE_TEMPLATE_ID = "TS3QzcDzxukJ9tjWsVhdGt";
    public static final String PACK_ASSOCIATE_ROLE_TEMPLATE_ID = "3LwAFLAyTFTy73CocXUDrd";


    public static String getCedarTemplateIdFromHttpPath(String httpPath) {
        switch (httpPath.toLowerCase()) {
            case "put /store/{store-id}/store_manager/{employee-id}":
                return STORE_MANAGER_ROLE_TEMPLATE_ID;
            case "put /store/{store-id}/pack_associate/{employee-id}":
                return PACK_ASSOCIATE_ROLE_TEMPLATE_ID;
            default:
                throw new InternalError("No role found for " + httpPath + " in RoleCedarTemplates");
        }
    }
}
