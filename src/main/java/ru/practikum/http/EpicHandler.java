package ru.practikum.http;

import com.google.gson.Gson;
import ru.practikum.manager.TaskManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.model.Epic;

import java.io.IOException;
import java.util.List;

public class EpicHandler extends BaseHttpHandler implements HttpHandler {

    public EpicHandler(TaskManager manager, Gson gson) {
        super(manager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();

            if ("GET".equals(method)) {
                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));
                    Epic epic = manager.getEpicById(id);
                    if (epic == null) sendNotFound(exchange);
                    else sendText(exchange, gson.toJson(epic), 200);
                } else {
                    List<Epic> epics = manager.getAllEpics();
                    sendText(exchange, gson.toJson(epics), 200);
                }
            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                if (body.isEmpty()) {
                    sendServerError(exchange);
                    return;
                }
                Epic epic = gson.fromJson(body, Epic.class);
                if (epic.getId() == 0) {
                    manager.createEpic(epic);
                } else {
                    manager.updateEpic(epic);
                }
                sendText(exchange, "{}", 201);
            } else if ("DELETE".equals(method)) {
                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));
                    manager.deleteEpicById(id);
                } else {
                    manager.deleteEpics();
                }
                sendText(exchange, "{}", 200);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            sendServerError(exchange);
        }
    }
}
