package example;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamodbEditItemHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveChamarUpdateERetornar200() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        when(mockDdb.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mockDdb);

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            String body = "{ \"field\":\"X\",\"value\":42 }";
            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("listId","l1","itemId","i1"))
                    .withBody(body);

            DynamodbEditItemHandler handler = new DynamodbEditItemHandler();
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(200, resp.getStatusCode());
            JsonNode root = mapper.readTree(resp.getBody());
            assertEquals("Item atualizado com sucesso", root.get("message").asText());

            ArgumentCaptor<UpdateItemRequest> cap =
                    ArgumentCaptor.forClass(UpdateItemRequest.class);
            verify(mockDdb).updateItem(cap.capture());
            UpdateItemRequest req = cap.getValue();

            assertTrue(req.updateExpression().contains("#field = :field"));
            assertTrue(req.updateExpression().contains("#value = :value"));
        }
    }

    @Test
    void handleRequest_semCamposParaAtualizar_deveRetornar400() throws Exception {
        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mock(DynamoDbClient.class));

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("listId","l1","itemId","i1"))
                    .withBody("{}");

            DynamodbEditItemHandler handler = new DynamodbEditItemHandler();
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(400, resp.getStatusCode());
            JsonNode root = mapper.readTree(resp.getBody());
            assertEquals("Nenhum campo para atualizar", root.get("error").asText());
        }
    }
}