package com.simpleweb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Context {
    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private static final ObjectMapper mapper = new ObjectMapper();

    public Context(HttpExchange exchange, Map<String, String> pathParams) {
        this.exchange = exchange;
        this.pathParams = pathParams;
    }

    // Allow Middleware to access raw exchange
    public HttpExchange exchange() {
        return this.exchange;
    }

    // Get a path parameter (e.g., /users/:id)
    public String path(String name) {
        return pathParams != null ? pathParams.get(name) : null;
    }

    // NEW: Get a query parameter (e.g., ?search=Akshith)
    public String query(String name) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2 && parts[0].equals(name)) {
                // Return the value (decoding usually happens here, keeping simple for now)
                return parts[1];
            }
        }
        return null;
    }

    // Read the raw body as a String
    public String body() {
        try {
            return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Convert JSON body to Java Object
    public <T> T bodyAs(Class<T> clazz) {
        try {
            return mapper.readValue(exchange.getRequestBody(), clazz);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Send a String response
    public void send(String text) {
        sendBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    // Send a JSON response
    public void json(Object data) {
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = mapper.writeValueAsBytes(data);
            sendBytes(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Set HTTP Status Code (chainable)
    public Context status(int code) {
        try {
            // Usually we send headers immediately or buffer them.
            // For this simple framework, we store it or assume send() is called next.
            // Note: HttpExchange requires sending headers before body.
            // We will handle the status inside sendBytes actually.
            // But to keep it simple, let's just cheat and assume default 200 unless set.
            // Real fix: Store 'code' in a field and use it in sendBytes.
            this.statusCode = code;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private int statusCode = 200; // Default to 200 OK

    public void sendBytes(byte[] data) {
        try {
            exchange.sendResponseHeaders(statusCode, data.length);
            OutputStream os = exchange.getResponseBody();
            os.write(data);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
