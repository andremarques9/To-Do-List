package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class DynamodbAddItemHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        try {
            String listId = event.getPathParameters() != null
                    ? event.getPathParameters().get("listId")
                    : null;
            if (listId == null || listId.isBlank()) {
                return createResponse(400, Map.of("error","'listId' é obrigatório na rota"));
            }

            String body = event.getBody();
            if (body == null || body.isBlank()) {
                return createResponse(400, Map.of("error","Corpo JSON é obrigatório"));
            }

            Map<String,Object> payload = mapper.readValue(body, new TypeReference<>() {});
            Object idObj = payload.remove("id");
            if (idObj == null || idObj.toString().isBlank()) {
                return createResponse(400, Map.of("error","Campo 'id' é obrigatório"));
            }
            String itemId = idObj.toString();

            Map<String,AttributeValue> item = new HashMap<>();
            item.put("PK", AttributeValue.builder().s(listId).build());
            item.put("SK", AttributeValue.builder().s("ITEM#" + itemId).build());

            payload.forEach((k,v) -> {
                if (v != null) {
                    AttributeValue av = (v instanceof Number)
                            ? AttributeValue.builder().n(v.toString()).build()
                            : AttributeValue.builder().s(v.toString()).build();
                    item.put(k, av);
                }
            });

            ddb.putItem(PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build()
            );

            return createResponse(201, Map.of(
                    "message","Item criado com sucesso",
                    "id", itemId
            ));

        } catch (Exception e) {
            context.getLogger().log("Erro ao inserir item: " + e.getMessage());
            return createResponse(500, Map.of("error","Falha interna ao criar item"));
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int status, Object bodyObj) {
        try {
            String body = mapper.writeValueAsString(bodyObj);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(Map.of("Content-Type","application/json"))
                    .withBody(body);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-Type","application/json"))
                    .withBody("{\"error\":\"Falha interna\"}");
        }
    }
}