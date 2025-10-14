package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamodbListItemsHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleRequest_queryPorChavePrimariaSemGsi() throws Exception {
        DynamoDbClient mockDdb = mock(DynamoDbClient.class);

        Map<String, AttributeValue> rawItem = Map.of(
                "PK",    AttributeValue.builder().s("USER#1").build(),
                "SK",    AttributeValue.builder().s("ORDER#abc").build(),
                "nome",  AttributeValue.builder().s("NomeX").build(),
                "preco", AttributeValue.builder().n("10.5").build()
        );

        QueryResponse qrMock = QueryResponse.builder()
                .items(List.of(rawItem))
                .build();
        when(mockDdb.query(any(QueryRequest.class))).thenReturn(qrMock);

        DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
        when(mockBuilder.build()).thenReturn(mockDdb);
        try (MockedStatic<DynamoDbClient> ddbStatic = mockStatic(DynamoDbClient.class)) {
            ddbStatic.when(DynamoDbClient::builder).thenReturn(mockBuilder);

            LambdaLogger mockLogger = mock(LambdaLogger.class);
            Context mockContext = mock(Context.class);
            when(mockContext.getLogger()).thenReturn(mockLogger);

            APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                    .withQueryStringParameters(Map.of("userId", "USER#1"));

            DynamodbListItemsHandler handler = new DynamodbListItemsHandler();
            APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

            assertEquals(200, response.getStatusCode());
            assertEquals("application/json", response.getHeaders().get("Content-Type"));

            JsonNode root = mapper.readTree(response.getBody());
            assertTrue(root.isArray());
            JsonNode first = root.get(0);
            assertEquals("USER#1", first.get("PK").asText());
            assertEquals("NomeX",  first.get("nome").asText());
            assertEquals(10.5,      first.get("preco").asDouble(), 0.001);

            ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
            verify(mockDdb).query(captor.capture());
            QueryRequest sent = captor.getValue();

            assertEquals("MySingleTable", sent.tableName());
            assertNull(sent.indexName());
            assertTrue(sent.keyConditionExpression().contains("PK = :pkVal"));
            assertTrue(sent.keyConditionExpression().contains("begins_with(SK, :skPrefix)"));

            Map<String, AttributeValue> exprValues = sent.expressionAttributeValues();
            assertEquals("USER#1", exprValues.get(":pkVal").s());
            assertEquals("ORDER#", exprValues.get(":skPrefix").s());
        }
    }
}