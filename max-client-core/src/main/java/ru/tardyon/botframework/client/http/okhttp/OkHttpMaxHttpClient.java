package ru.tardyon.botframework.client.http.okhttp;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.tardyon.botframework.client.error.MaxTransportException;
import ru.tardyon.botframework.client.http.HttpMethod;
import ru.tardyon.botframework.client.http.MaxHttpClient;
import ru.tardyon.botframework.client.http.MaxHttpRequest;
import ru.tardyon.botframework.client.http.MaxHttpResponse;

/**
 * OkHttp implementation of raw MAX HTTP transport.
 */
public final class OkHttpMaxHttpClient implements MaxHttpClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final URI baseUri;
    private final OkHttpClient httpClient;

    public OkHttpMaxHttpClient(URI baseUri, OkHttpClient httpClient) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public MaxHttpResponse execute(MaxHttpRequest request) {
        Request okhttpRequest = toOkHttpRequest(request);
        try (Response response = httpClient.newCall(okhttpRequest).execute()) {
            byte[] responseBody = response.body() == null ? new byte[0] : response.body().bytes();
            return new MaxHttpResponse(response.code(), toHeaders(response), responseBody);
        } catch (IOException e) {
            throw new MaxTransportException("HTTP transport call to MAX API failed", e);
        }
    }

    private Request toOkHttpRequest(MaxHttpRequest request) {
        String resolvedUrl = baseUri.resolve(normalizePath(request.path())).toString();
        Request.Builder builder = new Request.Builder().url(resolvedUrl);

        request.headers().forEach(builder::addHeader);

        return switch (request.method()) {
            case GET -> {
                if (request.body().length > 0) {
                    throw new IllegalArgumentException("GET request must not have body");
                }
                yield builder.get().build();
            }
            case POST -> builder.post(toRequestBody(request.body())).build();
            case PUT -> builder.put(toRequestBody(request.body())).build();
            case PATCH -> builder.patch(toRequestBody(request.body())).build();
            case DELETE -> {
                RequestBody body = request.body().length == 0 ? null : toRequestBody(request.body());
                yield builder.delete(body).build();
            }
        };
    }

    private static RequestBody toRequestBody(byte[] body) {
        return RequestBody.create(body, JSON_MEDIA_TYPE);
    }

    private static Map<String, List<String>> toHeaders(Response response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        response.headers().toMultimap().forEach(headers::put);
        return headers;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
