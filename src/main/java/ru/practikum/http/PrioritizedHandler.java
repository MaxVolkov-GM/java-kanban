package ru.practikum.http;

import com.google.gson.Gson;
import ru.practikum.manager.TaskManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class PrioritizedHandler extends BaseHttpHandler implements HttpHandler {

    public PrioritizedHandler(TaskManager manager, Gson gson) {
        super(manager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendText(exchange, gson.toJson(manager.getPrioritizedTasks()), 200);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            sendServerError(exchange);
        }
    }
}
