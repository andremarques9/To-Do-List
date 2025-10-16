package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamodbListItemsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        String listId = event.getPathParameters() != null
                ? event.getPathParameters().get("listId")
                : null;
        if (listId == null || listId.isBlank()) {
            return createResponse(400, Map.of("error","'listId' é obrigatório na rota"));
        }

        QueryRequest qr = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("PK = :pk AND begins_with(SK, :prefix)")
                .expressionAttributeValues(Map.of(
                        ":pk",     AttributeValue.builder().s(listId).build(),
                        ":prefix", AttributeValue.builder().s("ITEM#").build()
                ))
                .build();

        List<Map<String,Object>> items = ddb.query(qr)
                .items()
                .stream()
                .map(this::toSimpleMap)
                .collect(Collectors.toList());

        return createResponse(200, items);
    }

    private Map<String,Object> toSimpleMap(Map<String,AttributeValue> raw) {
        return raw.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    var v = e.getValue();
                    if (v.s() != null) return v.s();
                    if (v.n() != null) return Double.parseDouble(v.n());
                    if (v.bool() != null) return v.bool();
                    return null;
                }
        ));
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