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

public class DynamodbListListsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        try {
            QueryRequest qr = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("PK = :pk")
                    .expressionAttributeValues(Map.of(
                            ":pk", AttributeValue.builder().s("LISTS").build()
                    ))
                    .build();

            List<Map<String,Object>> lists = ddb.query(qr)
                    .items()
                    .stream()
                    .map(this::toSimpleMap)
                    .collect(Collectors.toList());

            return createResponse(200, lists);

        } catch (Exception e) {
            context.getLogger().log("Erro ao listar listas: " + e.getMessage());
            return createResponse(500, Map.of("error","Falha interna ao listar listas"));
        }
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