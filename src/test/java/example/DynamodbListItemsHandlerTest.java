package example;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamodbListItemsHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveRetornarItens() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        Map<String,AttributeValue> raw = Map.of(
                "PK",    AttributeValue.builder().s("l1").build(),
                "SK",    AttributeValue.builder().s("ITEM#i1").build(),
                "name",  AttributeValue.builder().s("N1").build(),
                "value", AttributeValue.builder().n("9").build()
        );
        when(mockDdb.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(raw)).build());

        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mockDdb);

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent()
                    .withPathParameters(Map.of("listId","l1"));

            DynamodbListItemsHandler handler = new DynamodbListItemsHandler();
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(200, resp.getStatusCode());
            assertEquals("application/json", resp.getHeaders().get("Content-Type"));

            JsonNode arr = mapper.readTree(resp.getBody());
            assertTrue(arr.isArray());
            assertEquals(1, arr.size());

            JsonNode item = arr.get(0);
            assertEquals("l1",      item.get("PK").asText());
            assertEquals("ITEM#i1", item.get("SK").asText());
            assertEquals("N1",      item.get("name").asText());
            assertEquals(9.0,       item.get("value").asDouble(), 1e-6);

            ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
            verify(mockDdb).query(captor.capture());
            QueryRequest qr = captor.getValue();

            assertTrue(qr.keyConditionExpression().startsWith("PK ="));
            assertTrue(qr.keyConditionExpression().contains("begins_with(SK"));
        }
    }

    @Test
    void handleRequest_semListId_deveRetornar400() throws Exception {
        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mock(DynamoDbClient.class));

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            DynamodbListItemsHandler handler = new DynamodbListItemsHandler();
            APIGatewayProxyResponseEvent resp =
                    handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

            assertEquals(400, resp.getStatusCode());
            JsonNode err = mapper.readTree(resp.getBody());
            assertEquals("'listId' é obrigatório na rota", err.get("error").asText());
        }
    }
}