package org.example.resourceEntityBuilders;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.verifiedpermissions.model.EntitiesDefinition;
import com.amazonaws.services.verifiedpermissions.model.EntityIdentifier;
import com.amazonaws.services.verifiedpermissions.model.EntityItem;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface ResourceEntityBuilder {
    public Pair<EntityIdentifier, EntitiesDefinition> getResourceEntities(APIGatewayV2HTTPEvent event);

}
