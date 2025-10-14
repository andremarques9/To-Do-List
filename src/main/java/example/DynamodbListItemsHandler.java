package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class DynamodbListItemsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient DDB = DynamoDbClient.builder().build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context ctx) {

        Map<String,String> qs = event.getQueryStringParameters();
        if (qs == null || !qs.containsKey("userId") || qs.get("userId").isBlank()) {
            return jsonError(400, "Parâmetro 'userId' é obrigatório");
        }
        String userId = qs.get("userId");


        QueryRequest qr = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("PK = :pkVal AND begins_with(SK, :skPrefix)")
                .expressionAttributeValues(Map.of(
                        ":pkVal",    AttributeValue.builder().s(userId).build(),
                        ":skPrefix", AttributeValue.builder().s("ORDER#").build()
                ))
                .build();

        List<Map<String, Object>> parsedItems = DDB.query(qr)
                .items()
                .stream()
                .map(this::convertAttributeMap)
                .toList();

        try {
            String body = MAPPER.writeValueAsString(parsedItems);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(body);

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\":\"Falha ao serializar resposta\"}");
        }
    }

    private Map<String, Object> convertAttributeMap(Map<String, AttributeValue> rawItem) {
        Map<String, Object> result = new HashMap<>();
        rawItem.forEach((key, val) -> {
            if (val.s() != null)         result.put(key, val.s());
            else if (val.n() != null)    result.put(key, Double.parseDouble(val.n()));
            else if (val.bool() != null) result.put(key, val.bool());
            else                         result.put(key, val.toString());
        });
        return result;
    }
    private APIGatewayProxyResponseEvent jsonResponse(int status, Object bodyObj) {
        try {
            String body = MAPPER.writeValueAsString(bodyObj);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*"
                    ))
                    .withBody(body);
        } catch (Exception e) {
            // Falha de serialização
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"error\":\"Falha de serialização\"}");
        }
    }

    // Monta uma resposta de erro padronizada
    private APIGatewayProxyResponseEvent jsonError(int status, String message) {
        return jsonResponse(status, Map.of("error", message));
    }
}