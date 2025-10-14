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

public class DynamodbEditItemHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient ddb = DynamoDbClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        try {
            context.getLogger().log("PathParameters raw: " + event.getPathParameters());
            context.getLogger().log("Body raw: " + event.getBody());

            String id = null;
            Map<String, String> pathParams = event.getPathParameters();
            if (pathParams != null && pathParams.containsKey("id")) {
                id = pathParams.get("id");
            }

            Map<String, Object> bodyMap = null;
            if (id == null && event.getBody() != null && !event.getBody().isBlank()) {
                bodyMap = mapper.readValue(
                        event.getBody(), new TypeReference<>() {});
                if (bodyMap.containsKey("id")) {
                    id = bodyMap.get("id").toString();
                }
            }

            if (id == null || id.isBlank()) {
                return createResponse(400,
                        "{\"error\":\"Campo 'id' é obrigatório (rota ou corpo)\"}");
            }

            if (bodyMap == null) {
                bodyMap = mapper.readValue(
                        event.getBody(), new TypeReference<>() {});
            }
            bodyMap.remove("id");
            if (bodyMap.isEmpty()) {
                return createResponse(400,
                        "{\"error\":\"Nenhum campo para atualizar\"}");
            }

            Map<String, String> exprNames = new HashMap<>();
            Map<String, AttributeValue> exprValues = new HashMap<>();
            StringJoiner updates = new StringJoiner(", ", "SET ", "");

            for (Map.Entry<String, Object> entry : bodyMap.entrySet()) {
                String key   = entry.getKey();
                Object value = entry.getValue();

                String nameKey  = "#" + key;
                String valueKey = ":" + key;
                exprNames.put(nameKey, key);

                AttributeValue av;
                if (value instanceof Number) {
                    av = AttributeValue.builder().n(value.toString()).build();
                } else if (value instanceof Boolean) {
                    av = AttributeValue.builder().bool((Boolean) value).build();
                } else {
                    av = AttributeValue.builder().s(value.toString()).build();
                }
                exprValues.put(valueKey, av);
                updates.add(nameKey + " = " + valueKey);
            }

            Map<String, AttributeValue> key = Map.of(
                    "PK", AttributeValue.builder().s(id).build(),
                    "SK", AttributeValue.builder().s("METADATA").build()
            );

            UpdateItemRequest req = UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .updateExpression(updates.toString())
                    .expressionAttributeNames(exprNames)
                    .expressionAttributeValues(exprValues)
                    .build();

            ddb.updateItem(req);

            String responseBody = mapper.writeValueAsString(
                    Map.of("message", "Item atualizado com sucesso", "id", id)
            );
            return createResponse(200, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Erro ao atualizar item: " + e.getMessage());
            return createResponse(500,
                    "{\"error\":\"Falha interna ao atualizar item\"}");
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}