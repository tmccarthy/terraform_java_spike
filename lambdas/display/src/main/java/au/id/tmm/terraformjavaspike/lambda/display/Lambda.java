package au.id.tmm.terraformjavaspike.lambda.display;

import au.id.tmm.terraformjavaspike.lambda.display.Lambda.Request;
import au.id.tmm.terraformjavaspike.lambda.display.Lambda.Response;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Lambda implements RequestHandler<Request, Response> {

    public Response handleRequest(Request request, Context context) {
        return new Response("Hello World!");
    }

    public static final class Request {
    }

    public static final class Response {
        private final String body;

        public Response(String body) {
            this.body = body;
        }

        public String getBody() {
            return body;
        }
    }
}