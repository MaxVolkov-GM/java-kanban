package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practikum.http.BaseHttpHandler;
import ru.practikum.manager.TaskManager;

import java.io.IOException;

public class HistoryHandler extends BaseHttpHandler {

    public HistoryHandler(TaskManager manager, Gson gson) {
        super(manager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendText(exchange, gson.toJson(manager.getHistory()), 200);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }
}
