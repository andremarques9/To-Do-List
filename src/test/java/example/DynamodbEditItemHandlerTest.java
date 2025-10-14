package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamodbEditItemHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveChamarUpdateItemERetornar200() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        when(mockDdb.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::create).thenReturn(mockDdb);

            LambdaLogger mockLogger = mock(LambdaLogger.class);
            Context mockContext = mock(Context.class);
            when(mockContext.getLogger()).thenReturn(mockLogger);

            String bodyJson = """
                {
                  "nome": "Atualizado",
                  "preco": 29.9
                }
                """;
            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("id", "abc-123"))
                    .withBody(bodyJson);

            DynamodbEditItemHandler handler = new DynamodbEditItemHandler();
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

            assertEquals(200, response.getStatusCode());
            assertEquals("application/json", response.getHeaders().get("Content-Type"));

            JsonNode root = mapper.readTree(response.getBody());
            assertEquals("Item atualizado com sucesso", root.get("message").asText());
            assertEquals("abc-123",                    root.get("id").asText());

            ArgumentCaptor<UpdateItemRequest> captor =
                    ArgumentCaptor.forClass(UpdateItemRequest.class);
            verify(mockDdb).updateItem(captor.capture());
            UpdateItemRequest sent = captor.getValue();

            Map<String, AttributeValue> key = sent.key();
            assertEquals("abc-123", key.get("PK").s());
            assertEquals("METADATA",key.get("SK").s());

            String expr = sent.updateExpression();
            assertTrue(expr.startsWith("SET "));
            assertTrue(expr.contains("#nome = :nome"));
            assertTrue(expr.contains("#preco = :preco"));

            Map<String, AttributeValue> vals = sent.expressionAttributeValues();
            assertEquals("Atualizado", vals.get(":nome").s());
            assertEquals("29.9",        vals.get(":preco").n());
        }
    }

    @Test
    void handleRequest_semCamposParaAtualizar_deveRetornar400() {
        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::create)
                    .thenReturn(mock(DynamoDbClient.class));

            LambdaLogger mockLogger = mock(LambdaLogger.class);
            Context mockContext = mock(Context.class);
            when(mockContext.getLogger()).thenReturn(mockLogger);

            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("id", "abc-123"))
                    .withBody("{}");

            DynamodbEditItemHandler handler = new DynamodbEditItemHandler();
            APIGatewayProxyResponseEvent response =
                    handler.handleRequest(event, mockContext);

            assertEquals(400, response.getStatusCode());
            assertTrue(response.getBody().contains("Nenhum campo para atualizar"));
        }
    }
}