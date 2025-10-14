package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Hello implements RequestHandler<Object, String> {

    public String handleRequest(final Object input, final Context context) {

        System.out.println(input);
        return "Hello World!";
    }
}