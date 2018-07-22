package au.id.tmm.terraformjavaspike.counter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.IOUtils;

import java.nio.charset.Charset;
import java.util.OptionalInt;

public final class Main {

    private static final String OBJECT_KEY = "object";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

        String bucketName = args[0];

        int previousCount = existingCount(s3, bucketName).orElse(0);
        int newCount = previousCount + 1;

        s3.putObject(bucketName, OBJECT_KEY, Integer.toString(newCount));
    }

    private static OptionalInt existingCount(AmazonS3 s3, String bucketName) {
        try (S3Object object = s3.getObject(bucketName, OBJECT_KEY);
             S3ObjectInputStream inputStream = object.getObjectContent()) {

            String objectContent = IOUtils.toString(inputStream, Charset.forName("UTF-8"));

            return OptionalInt.of(Integer.parseInt(objectContent));
        } catch (Exception e) {
            return OptionalInt.empty();
        }
    }

}
