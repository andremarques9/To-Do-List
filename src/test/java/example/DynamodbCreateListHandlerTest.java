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


class DynamodbCreateListHandlerTest {

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

            DynamodbCreateListHandler handler = new DynamodbCreateListHandler();

            String body = """
        {
          "id":"list-1",
          "title":"Minha Lista",
          "priority":5
        }
        """;
            APIGatewayProxyRequestEvent ev = new APIGatewayProxyRequestEvent().withBody(body);
            APIGatewayProxyResponseEvent resp = handler.handleRequest(ev, null);

            assertEquals(201, resp.getStatusCode());
            assertEquals("application/json", resp.getHeaders().get("Content-Type"));

            JsonNode root = mapper.readTree(resp.getBody());
            assertEquals("Lista criada com sucesso", root.get("message").asText());
            assertEquals("list-1", root.get("id").asText());

            ArgumentCaptor<PutItemRequest> cap = ArgumentCaptor.forClass(PutItemRequest.class);
            verify(mockDdb).putItem(cap.capture());
            PutItemRequest req = cap.getValue();

            assertEquals("MySingleTable", req.tableName());
            Map<String,AttributeValue> item = req.item();
            assertEquals("LISTS",           item.get("PK").s());
            assertEquals("list-1",          item.get("SK").s());
            assertEquals("Minha Lista",     item.get("title").s());
            assertEquals("5",               item.get("priority").n());
            assertTrue(item.containsKey("createdAt"));
        }
    }
}