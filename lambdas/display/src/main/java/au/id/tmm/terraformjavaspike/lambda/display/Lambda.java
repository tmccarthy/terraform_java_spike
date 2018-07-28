package au.id.tmm.terraformjavaspike.lambda.display;

import au.id.tmm.terraformjavaspike.lambda.display.Lambda.Request;
import au.id.tmm.terraformjavaspike.lambda.display.Lambda.Response;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Lambda implements RequestHandler<Request, Response> {

    private static final String OBJECT_KEY = "object";

    @Override
    public Response handleRequest(Request request, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            logger.log("Received request " + request);

            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

            String bucketName = System.getenv("BUCKET_NAME");

            logger.log("BUCKET_NAME=" + bucketName);
            logger.log("OBJECT_KEY=" + OBJECT_KEY);

            int currentCount = Integer.parseInt(s3Client.getObjectAsString(bucketName, OBJECT_KEY));

            int nextCount = currentCount + 1;

            s3Client.putObject(bucketName, OBJECT_KEY, Integer.toString(nextCount));

            return new Response(Integer.toString(currentCount));
        } catch (Throwable t) {
            String message = Stream.of(t.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n\t"));

            logger.log(message);

            if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new RuntimeException(t);
            }
        }
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