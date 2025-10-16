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

class DynamodbAddItemHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveChamarPutItemERetornar201() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        when(mockDdb.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mockDdb);

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            String body = """
        { "id":"i1","name":"X","value":9 }
        """;
            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("listId","l1"))
                    .withBody(body);

            DynamodbAddItemHandler handler = new DynamodbAddItemHandler();
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(201, resp.getStatusCode());
            JsonNode root = mapper.readTree(resp.getBody());
            assertEquals("Item criado com sucesso", root.get("message").asText());
            assertEquals("i1", root.get("id").asText());

            ArgumentCaptor<PutItemRequest> cap = ArgumentCaptor.forClass(PutItemRequest.class);
            verify(mockDdb).putItem(cap.capture());
            PutItemRequest req = cap.getValue();
            Map<String,AttributeValue> item = req.item();
            assertEquals("l1",        item.get("PK").s());
            assertEquals("ITEM#i1",   item.get("SK").s());
        }
    }

    @Test
    void handleRequest_semBody_deveRetornar400() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mockDdb);

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("listId","l1"));

            APIGatewayProxyResponseEvent resp =
                    new DynamodbAddItemHandler().handleRequest(ev, null);

            assertEquals(400, resp.getStatusCode());
            JsonNode root = mapper.readTree(resp.getBody());
            assertEquals("Corpo JSON é obrigatório", root.get("error").asText());
        }
    }
}