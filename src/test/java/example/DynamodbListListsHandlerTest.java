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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamodbListListsHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_deveRetornarListas() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);
        Map<String,AttributeValue> raw = Map.of(
                "PK",    AttributeValue.builder().s("LISTS").build(),
                "SK",    AttributeValue.builder().s("l1").build(),
                "title", AttributeValue.builder().s("T1").build()
        );
        when(mockDdb.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().items(List.of(raw)).build());

        DynamoDbClientBuilder fakeBuilder = mock(DynamoDbClientBuilder.class);
        when(fakeBuilder.build()).thenReturn(mockDdb);

        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(fakeBuilder);

            DynamodbListListsHandler handler = new DynamodbListListsHandler();
            APIGatewayProxyResponseEvent resp =
                    handler.handleRequest(new APIGatewayProxyRequestEvent(), null);

            assertEquals(200, resp.getStatusCode());
            assertEquals("application/json", resp.getHeaders().get("Content-Type"));

            JsonNode arr = mapper.readTree(resp.getBody());
            assertTrue(arr.isArray());
            JsonNode first = arr.get(0);
            assertEquals("l1", first.get("SK").asText());
            assertEquals("T1", first.get("title").asText());

            ArgumentCaptor<QueryRequest> cap =
                    ArgumentCaptor.forClass(QueryRequest.class);
            verify(mockDdb).query(cap.capture());
            QueryRequest qr = cap.getValue();

            assertTrue(qr.keyConditionExpression().contains("PK = :pk"));
        }
    }
}