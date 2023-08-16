package org.example.resourceEntityBuilders;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.verifiedpermissions.model.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.example.config.EntityTypesConstants;

import java.util.ArrayList;
import java.util.List;

public class OrderResourceEntitiesBuilder implements ResourceEntityBuilder
{
    private static final EntityItem ordersParentEntity = new EntityItem().withIdentifier(
            new EntityIdentifier().
                    withEntityType("avp::sample::toy::store::AllOrders").
                    withEntityId("all-orders")
    );
    @Override
    public Pair<EntityIdentifier, EntitiesDefinition> getResourceEntities(APIGatewayV2HTTPEvent event) {
        EntityIdentifier orderEntityIdentifier = getOrderEntityIdentifier(event);
        EntitiesDefinition resourceEntities = getStoreAndOrderEntity(event);
        return ImmutablePair.of(orderEntityIdentifier, resourceEntities);
    }

    private EntityIdentifier getOrderEntityIdentifier(APIGatewayV2HTTPEvent event) {
        EntityIdentifier resourceIdentifier = new EntityIdentifier();
        resourceIdentifier.setEntityType(EntityTypesConstants.ORDER_ENTITY_TYPE);
        resourceIdentifier.setEntityId(event.getPathParameters().get("order-id"));
        return resourceIdentifier;
    }


    private EntitiesDefinition getStoreAndOrderEntity(APIGatewayV2HTTPEvent event) {

        Pair<EntityIdentifier, EntitiesDefinition> storeEntities = new StoreResourceEntityBuilder().getResourceEntities(event);

        EntityItem orderEntity = new EntityItem();
        orderEntity.setIdentifier(new EntityIdentifier().
                withEntityType(EntityTypesConstants.ORDER_ENTITY_TYPE).
                withEntityId(event.getPathParameters().get("order-id")));
        orderEntity.setParents(List.of(storeEntities.getLeft(),  ordersParentEntity.getIdentifier()));
        List<EntityItem> entityItems = new ArrayList<>();
        entityItems.add(orderEntity);
        entityItems.addAll(storeEntities.getRight().getEntityList());
        entityItems.add(ordersParentEntity);
        return new EntitiesDefinition().withEntityList(entityItems);
    }


}
