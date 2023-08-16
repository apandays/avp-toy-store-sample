package org.example.resourceEntityBuilders;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.verifiedpermissions.model.EntitiesDefinition;
import com.amazonaws.services.verifiedpermissions.model.EntityIdentifier;
import com.amazonaws.services.verifiedpermissions.model.EntityItem;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.example.config.EntityTypesConstants;

import java.util.Collections;

public class StoreResourceEntityBuilder implements ResourceEntityBuilder {

    private static final EntityItem storeParentEntity = new EntityItem().withIdentifier(
            new EntityIdentifier().
                    withEntityType("avp::sample::toy::store::AllStores").
                    withEntityId("all-stores")
    );
    @Override
    public Pair<EntityIdentifier, EntitiesDefinition> getResourceEntities(APIGatewayV2HTTPEvent event) {

            EntityItem storeEntity = new EntityItem();
            storeEntity.setIdentifier(new EntityIdentifier().
                    withEntityType(EntityTypesConstants.STORE_ENTITY_TYPE).
                    withEntityId(event.getPathParameters().get("store-id")));
            storeEntity.setParents(Collections.singleton(storeParentEntity.getIdentifier()));
            return new ImmutablePair<>(storeEntity.getIdentifier(),
                    new EntitiesDefinition().withEntityList(storeEntity, storeParentEntity));
    }
}
