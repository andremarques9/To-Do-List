package example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelloTest {
    @Test
    void handleRequestMustReturnsHelloWorld() {
        Hello handler = new Hello();

        String result = handler.handleRequest("any value", null);

        assertEquals("Hello World!", result);
    }
}