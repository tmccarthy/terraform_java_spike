package au.id.tmm.terraformjavaspike.lambda.display;

import au.id.tmm.terraformjavaspike.lambda.display.Lambda.Request;
import au.id.tmm.terraformjavaspike.lambda.display.Lambda.Response;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Optional;
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

            Optional<Integer> existingCount = existingCount(s3Client, bucketName);

            logger.log("existingCount=" + existingCount);

            return new Response(existingCount.map(i -> Integer.toString(i)).orElse("null"));
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

    private static Optional<Integer> existingCount(AmazonS3 s3, String bucketName) throws IOException {
        try (S3Object object = s3.getObject(bucketName, OBJECT_KEY);
             S3ObjectInputStream inputStream = object.getObjectContent()) {

            String objectContent = IOUtils.toString(inputStream, Charset.forName("UTF-8"));

            return Optional.of(Integer.parseInt(objectContent));
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