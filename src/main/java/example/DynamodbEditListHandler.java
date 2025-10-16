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

public class DynamodbEditListHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        try {
            String id = event.getPathParameters() != null
                    ? event.getPathParameters().get("id")
                    : null;

            if (id == null || id.isBlank()) {
                return createResponse(400, "{\"error\":\"Campo 'id' é obrigatório na rota\"}");
            }

            Map<String,Object> body = mapper.readValue(
                    event.getBody(), new TypeReference<>() {}
            );
            body.remove("id");
            if (body.isEmpty()) {
                return createResponse(400, "{\"error\":\"Nenhum campo para atualizar\"}");
            }

            Map<String,String> names  = new HashMap<>();
            Map<String,AttributeValue> values = new HashMap<>();
            StringJoiner expr = new StringJoiner(", ", "SET ", "");

            for (var e : body.entrySet()) {
                String key = e.getKey();
                String nameKey = "#" + key;
                String valKey  = ":" + key;
                names.put(nameKey, key);

                Object v = e.getValue();
                AttributeValue av = (v instanceof Number)
                        ? AttributeValue.builder().n(v.toString()).build()
                        : AttributeValue.builder().s(v.toString()).build();

                values.put(valKey, av);
                expr.add(nameKey + " = " + valKey);
            }

            var req = UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(
                            "PK", AttributeValue.builder().s(id).build(),
                            "SK", AttributeValue.builder().s("METADATA").build()
                    ))
                    .updateExpression(expr.toString())
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(values)
                    .build();

            ddb.updateItem(req);

            String resp = mapper.writeValueAsString(
                    Map.of("message","Lista atualizada com sucesso","id",id)
            );
            return createResponse(200, resp);

        } catch (Exception e) {
            context.getLogger().log("Erro ao atualizar lista: " + e.getMessage());
            return createResponse(500, "{\"error\":\"Falha interna ao atualizar lista\"}");
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int status, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of(
                        "Content-Type","application/json",
                        "Access-Control-Allow-Origin","*"
                ))
                .withBody(body);
    }
}