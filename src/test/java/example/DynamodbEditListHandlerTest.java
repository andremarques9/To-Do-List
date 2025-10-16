package example;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.*;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamodbEditListHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveChamarUpdateItemERetornar200() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        when(mockDdb.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mockDdb);

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            String body = """
        { "title":"Novo","priority":2 }
        """;
            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("id","l1"))
                    .withBody(body);

            DynamodbEditListHandler handler = new DynamodbEditListHandler();
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(200, resp.getStatusCode());
            JsonNode root = mapper.readTree(resp.getBody());
            assertEquals("Lista atualizada com sucesso", root.get("message").asText());

            ArgumentCaptor<UpdateItemRequest> cap =
                    ArgumentCaptor.forClass(UpdateItemRequest.class);
            verify(mockDdb).updateItem(cap.capture());
            UpdateItemRequest req = cap.getValue();
            assertTrue(req.updateExpression().contains("#title = :title"));
            assertTrue(req.updateExpression().contains("#priority = :priority"));
        }
    }

    @Test
    void handleRequest_semCamposParaAtualizar_deveRetornar400() {
        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mock(DynamoDbClient.class));

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("id","l1"))
                    .withBody("{}");

            DynamodbEditListHandler handler = new DynamodbEditListHandler();
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(400, resp.getStatusCode());
            assertTrue(resp.getBody().contains("Nenhum campo para atualizar"));
        }
    }
}