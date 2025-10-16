package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.stream.Collectors;

public class DynamodbGetListHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    //private static final String TABLE_NAME = System.getenv("TABLE_NAME");
    private static final String TABLE_NAME = "MySingleTable";
    private final DynamoDbClient ddb = DynamoDbClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        String id = event.getPathParameters() != null
                ? event.getPathParameters().get("id")
                : null;
        if (id == null || id.isBlank()) {
            return createResponse(400, Map.of("error","'id' é obrigatório na rota"));
        }

        GetItemRequest req = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(
                        "PK", AttributeValue.builder().s("LISTS").build(),
                        "SK", AttributeValue.builder().s(id).build()
                ))
                .build();

        var resp = ddb.getItem(req);
        if (!resp.hasItem() || resp.item().isEmpty()) {
            return createResponse(404, Map.of("error","Lista não encontrada"));
        }

        Map<String,Object> body = resp.item().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            var v = e.getValue();
                            if (v.s() != null) return v.s();
                            if (v.n() != null) return Double.parseDouble(v.n());
                            if (v.bool() != null) return v.bool();
                            return null;
                        }
                ));

        return createResponse(200, body);
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