package com.simpleweb;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniWeb {

    private int port;
    private HttpServer server;
    private final List<RouteEntry> routes = new ArrayList<>();

    // NEW: List to store middlewares
    private final List<Middleware> middlewares = new ArrayList<>();

    // ... (Keep inner class RouteEntry EXACTLY as it was) ...
    private static class RouteEntry {
        String method;
        Pattern pattern;
        Handler handler;
        List<String> paramNames;

        RouteEntry(String method, String path, Handler handler) {
            this.method = method;
            this.handler = handler;
            this.paramNames = new ArrayList<>();
            String regexPath = path;
            if (path.contains("*")) {
                StringBuilder sb = new StringBuilder("^");
                String[] parts = path.split("\\*");
                sb.append(parts[0]);
                if (parts.length > 1) {
                    String paramName = parts[1];
                    paramNames.add(paramName);
                    sb.append("(.*)");
                }
                sb.append("$");
                this.pattern = Pattern.compile(sb.toString());
            } else {
                Matcher m = Pattern.compile(":([a-zA-Z0-9_]+)").matcher(path);
                StringBuffer sb = new StringBuffer();
                sb.append("^");
                while (m.find()) {
                    paramNames.add(m.group(1));
                    m.appendReplacement(sb, "([^/]+)");
                }
                m.appendTail(sb);
                sb.append("$");
                this.pattern = Pattern.compile(sb.toString());
            }
        }
    }

    public MiniWeb(int port) {
        this.port = port;
    }

    // NEW: Allow users to add middleware
    public void use(Middleware middleware) {
        middlewares.add(middleware);
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.createContext("/", this::handleRequest);
            System.out.println("🚀 Framework started on port " + port);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        // We defer the logic to the chain executor
        executeMiddlewareChain(exchange, 0);
    }

    // NEW: Recursive function to run the middleware chain
    private void executeMiddlewareChain(HttpExchange exchange, int index) {
        // 1. If we finished all middlewares, run the actual Router logic
        if (index == middlewares.size()) {
            try {
                runRouterLogic(exchange);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // 2. Get the next middleware
        Middleware currentMiddleware = middlewares.get(index);

        // 3. Create a temporary Context (we might need a better way to share context later, but this works)
        // Note: In a real framework, we'd create the Context ONCE at the start.
        // For simplicity, let's assume Context is cheap to make, or we pass it down.
        // Actually, creating a new Context every time resets the params.
        // Let's create one Context and pass it?
        // For now, let's keep it simple: Create a context just for the middleware.
        // It won't have path params yet (because routing hasn't happened), but it has query/body.
        Context ctx = new Context(exchange, null);

        // 4. Run the middleware. We pass a "Runnable" that calls the NEXT middleware.
        currentMiddleware.handle(ctx, () -> {
            executeMiddlewareChain(exchange, index + 1);
        });
    }

    // Moved the old handleRequest logic here
    private void runRouterLogic(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        for (RouteEntry route : routes) {
            if (!route.method.equalsIgnoreCase(method)) continue;

            Matcher matcher = route.pattern.matcher(path);
            if (matcher.matches()) {
                Map<String, String> params = new HashMap<>();
                for (int i = 0; i < route.paramNames.size(); i++) {
                    params.put(route.paramNames.get(i), matcher.group(i + 1));
                }

                try {
                    route.handler.handle(new Context(exchange, params));
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                }
                return;
            }
        }

        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    public void get(String path, Handler handler) {
        routes.add(new RouteEntry("GET", path, handler));
    }

    public void post(String path, Handler handler) {
        routes.add(new RouteEntry("POST", path, handler));
    }
}
