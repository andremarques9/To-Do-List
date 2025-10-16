package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class DynamodbEditItemHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {

        try {
            Map<String,String> path = event.getPathParameters();
            String listId = path != null ? path.get("listId") : null;
            String itemId = path != null ? path.get("itemId") : null;
            if (listId == null || listId.isBlank()
                    || itemId == null || itemId.isBlank()) {
                return createResponse(400,
                        Map.of("error","'listId' e 'itemId' são obrigatórios na rota"));
            }

            String body = event.getBody();
            if (body == null || body.isBlank()) {
                return createResponse(400,
                        Map.of("error","Corpo JSON é obrigatório"));
            }

            Map<String,Object> payload = mapper.readValue(
                    body, new TypeReference<Map<String,Object>>() {});
            payload.remove("id");
            if (payload.isEmpty()) {
                return createResponse(400,
                        Map.of("error","Nenhum campo para atualizar"));
            }

            Map<String,String> exprNames  = new HashMap<>();
            Map<String,AttributeValue> exprValues = new HashMap<>();
            StringJoiner updates = new StringJoiner(", ", "SET ", "");

            for (var e : payload.entrySet()) {
                String key   = e.getKey();
                String nameK = "#" + key;
                String valK  = ":" + key;

                exprNames.put(nameK, key);

                Object v = e.getValue();
                AttributeValue av = (v instanceof Number)
                        ? AttributeValue.builder().n(v.toString()).build()
                        : AttributeValue.builder().s(v.toString()).build();
                exprValues.put(valK, av);

                updates.add(nameK + " = " + valK);
            }

            ddb.updateItem(UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(
                            "PK", AttributeValue.builder().s(listId).build(),
                            "SK", AttributeValue.builder().s("ITEM#" + itemId).build()
                    ))
                    .updateExpression(updates.toString())
                    .expressionAttributeNames(exprNames)
                    .expressionAttributeValues(exprValues)
                    .build()
            );

            return createResponse(200, Map.of(
                    "message","Item atualizado com sucesso",
                    "listId", listId,
                    "itemId", itemId
            ));

        } catch (Exception e) {
            context.getLogger().log("Erro ao atualizar item: " + e.getMessage());
            return createResponse(500,
                    Map.of("error","Falha interna ao atualizar item"));
        }
    }

    private APIGatewayProxyResponseEvent createResponse(
            int status,
            Map<String,Object> bodyObj) {

        try {
            String json = mapper.writeValueAsString(bodyObj);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(Map.of("Content-Type","application/json"))
                    .withBody(json);
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-Type","application/json"))
                    .withBody("{\"error\":\"Falha interna\"}");
        }
    }
}