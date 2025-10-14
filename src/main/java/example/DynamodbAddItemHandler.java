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

public class DynamodbAddItemHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient ddb = DynamoDbClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        try {
            if (event.getBody() == null || event.getBody().isBlank()) {
                return createResponse(400, "{\"error\":\"Corpo JSON é obrigatório\"}");
            }
            Map<String,Object> bodyMap = mapper.readValue(
                    event.getBody(), new TypeReference<>() {});

            Object idObj = bodyMap.remove("id");
            if (idObj == null || idObj.toString().isBlank()) {
                return createResponse(400, "{\"error\":\"Campo 'id' é obrigatório\"}");
            }
            String id = idObj.toString();

            Map<String,AttributeValue> item = new HashMap<>();
            item.put("PK", AttributeValue.builder().s(id).build());
            item.put("SK", AttributeValue.builder().s("METADATA").build());

            bodyMap.forEach((key, val) -> {
                if (val != null) {
                    AttributeValue av = (val instanceof Number)
                            ? AttributeValue.builder().n(val.toString()).build()
                            : AttributeValue.builder().s(val.toString()).build();
                    item.put(key, av);
                }
            });

            PutItemRequest putReq = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();
            ddb.putItem(putReq);

            String responseBody = mapper.writeValueAsString(
                    Map.of("message", "Item criado com sucesso", "id", id));
            return createResponse(201, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Erro ao inserir item: " + e.getMessage());
            return createResponse(500, "{\"error\":\"Falha interna ao criar item\"}");
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}