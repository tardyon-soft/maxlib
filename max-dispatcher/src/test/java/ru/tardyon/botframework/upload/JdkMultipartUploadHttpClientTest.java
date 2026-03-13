package ru.tardyon.botframework.upload;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JdkMultipartUploadHttpClientTest {

    @Test
    void buildsMultipartBodyWithFileHeadersAndPayload() {
        MultipartUploadRequest request = new MultipartUploadRequest(
                URI.create("https://upload.example.com/u-1"),
                "photo.png",
                "image/png",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        byte[] body = JdkMultipartUploadHttpClient.buildMultipartBody("boundary-1", request, "file");
        String bodyText = new String(body, StandardCharsets.UTF_8);

        assertTrue(bodyText.contains("Content-Disposition: form-data; name=\"file\"; filename=\"photo.png\""));
        assertTrue(bodyText.contains("Content-Type: image/png"));
        assertTrue(bodyText.contains("\r\ndata\r\n"));
        assertTrue(bodyText.contains("--boundary-1--"));
    }
}
