package ru.tardyon.botframework.upload;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * {@link MultipartUploadHttpClient} based on JDK {@link HttpClient}.
 */
public final class JdkMultipartUploadHttpClient implements MultipartUploadHttpClient {
    private static final String CRLF = "\r\n";

    private final HttpClient httpClient;
    private final String multipartFieldName;

    public JdkMultipartUploadHttpClient(HttpClient httpClient) {
        this(httpClient, "file");
    }

    public JdkMultipartUploadHttpClient(HttpClient httpClient, String multipartFieldName) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.multipartFieldName = Objects.requireNonNull(multipartFieldName, "multipartFieldName");
        if (multipartFieldName.isBlank()) {
            throw new IllegalArgumentException("multipartFieldName must not be blank");
        }
    }

    @Override
    public CompletionStage<MultipartUploadResponse> upload(MultipartUploadRequest request) {
        Objects.requireNonNull(request, "request");

        String boundary = "maxbot-" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, request, multipartFieldName);

        HttpRequest httpRequest = HttpRequest.newBuilder(request.uploadUrl())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> new MultipartUploadResponse(response.statusCode(), response.body()));
    }

    static byte[] buildMultipartBody(String boundary, MultipartUploadRequest request, String fieldName) {
        String preamble = "--" + boundary + CRLF
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + request.fileName() + "\"" + CRLF
                + "Content-Type: " + request.contentType() + CRLF
                + CRLF;
        String suffix = CRLF + "--" + boundary + "--" + CRLF;

        byte[] preambleBytes = preamble.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] payload = request.content();

        byte[] body = new byte[preambleBytes.length + payload.length + suffixBytes.length];
        System.arraycopy(preambleBytes, 0, body, 0, preambleBytes.length);
        System.arraycopy(payload, 0, body, preambleBytes.length, payload.length);
        System.arraycopy(suffixBytes, 0, body, preambleBytes.length + payload.length, suffixBytes.length);
        return body;
    }
}
