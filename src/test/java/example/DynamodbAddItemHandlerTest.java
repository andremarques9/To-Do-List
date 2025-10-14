package example;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamodbAddItemHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveChamarPutItemERetornar201() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        when(mockDdb.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::create).thenReturn(mockDdb);

            String jsonBody = """
                {
                  "id":"abc-123",
                  "nome":"Produto X",
                  "preco": 19.9
                }
                """;

            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                    .withBody(jsonBody);

            DynamodbAddItemHandler handler = new DynamodbAddItemHandler();
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

            assertEquals(201, response.getStatusCode());
            assertEquals("application/json", response.getHeaders().get("Content-Type"));

            JsonNode root = mapper.readTree(response.getBody());
            assertEquals("Item criado com sucesso", root.get("message").asText());
            assertEquals("abc-123", root.get("id").asText());

            ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
            verify(mockDdb).putItem(captor.capture());

            PutItemRequest putReq = captor.getValue();
            Map<String, AttributeValue> item = putReq.item();

            assertEquals("MySingleTable", putReq.tableName());
            assertEquals("abc-123",      item.get("PK").s());
            assertEquals("METADATA",     item.get("SK").s());
            assertEquals("Produto X",    item.get("nome").s());
            assertEquals("19.9",         item.get("preco").n());
        }
    }

    @Test
    void handleRequest_semBody_deveRetornar400() {
        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            // a princípio não precisamos mockar o client aqui
            ddbStatic.when(DynamoDbClient::create)
                    .thenReturn(mock(DynamoDbClient.class));

            APIGatewayProxyRequestEvent eventVazio = new APIGatewayProxyRequestEvent();
            DynamodbAddItemHandler handler = new DynamodbAddItemHandler();
            APIGatewayProxyResponseEvent resp = handler.handleRequest(eventVazio, null);

            assertEquals(400, resp.getStatusCode());
            assertTrue(resp.getBody().contains("Corpo JSON é obrigatório"));
        }
    }
}