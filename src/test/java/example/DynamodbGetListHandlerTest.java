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

class DynamodbGetListHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveRetornarListaExistente() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        Map<String,AttributeValue> item = Map.of(
                "PK",    AttributeValue.builder().s("LISTS").build(),
                "SK",    AttributeValue.builder().s("l1").build(),
                "title", AttributeValue.builder().s("T1").build()
        );
        when(mockDdb.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(item).build());

        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mockDdb);

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            DynamodbGetListHandler handler = new DynamodbGetListHandler();
            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("id","l1"));
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(200, resp.getStatusCode());
            JsonNode root = mapper.readTree(resp.getBody());
            assertEquals("T1", root.get("title").asText());

            ArgumentCaptor<GetItemRequest> cap = ArgumentCaptor.forClass(GetItemRequest.class);
            verify(mockDdb).getItem(cap.capture());
            GetItemRequest req = cap.getValue();
            assertEquals("MySingleTable", req.tableName());
            assertEquals("l1", req.key().get("SK").s());
        }
    }

    @Test
    void handleRequest_semId_deveRetornar400() {
        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mock(DynamoDbClient.class));

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            DynamodbGetListHandler handler = new DynamodbGetListHandler();
            APIGatewayProxyResponseEvent resp =
                    handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

            assertEquals(400, resp.getStatusCode());
            assertTrue(resp.getBody().contains("'id' é obrigatório"));
        }
    }
}