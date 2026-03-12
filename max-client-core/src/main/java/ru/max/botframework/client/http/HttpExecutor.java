package ru.max.botframework.client.http;

import java.io.IOException;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Executes raw HTTP requests against MAX API endpoints.
 */
public interface HttpExecutor {
    Response execute(Request request) throws IOException;
}
