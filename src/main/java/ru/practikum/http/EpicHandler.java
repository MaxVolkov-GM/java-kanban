package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Epic;

import java.io.IOException;

public class EpicHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public EpicHandler(TaskManager manager, Gson gson) {
        this.manager = manager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method)) {
                String[] segments = path.split("/");
                if (segments.length == 2 && segments[1].equals("epics")) {
                    sendText(exchange, gson.toJson(manager.getAllEpics()));
                } else if (segments.length == 3 && segments[1].equals("epics")) {
                    int id = Integer.parseInt(segments[2]);
                    Epic epic = manager.getEpicById(id);
                    if (epic != null) {
                        sendText(exchange, gson.toJson(epic));
                    } else {
                        sendText(exchange, "Epic not found", 404);
                    }
                } else {
                    sendServerError(exchange);
                }
            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                Epic epic = gson.fromJson(body, Epic.class);
                manager.createEpic(epic);
                sendText(exchange, "Epic created", 201);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }
}
