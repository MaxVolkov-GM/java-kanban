package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Subtask;

import java.io.IOException;
import java.util.List;

public class SubtaskHandler extends BaseHttpHandler implements HttpHandler {

    public SubtaskHandler(TaskManager manager, Gson gson) {
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
                    Subtask subtask = manager.getSubtaskById(id);
                    if (subtask == null) sendNotFound(exchange);
                    else sendText(exchange, gson.toJson(subtask), 200);
                } else {
                    List<Subtask> subtasks = manager.getAllSubtasks();
                    sendText(exchange, gson.toJson(subtasks), 200);
                }
            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                if (body.isEmpty()) {
                    sendServerError(exchange);
                    return;
                }
                Subtask subtask = gson.fromJson(body, Subtask.class);
                try {
                    if (subtask.getId() == 0) manager.createSubtask(subtask);
                    else manager.updateSubtask(subtask);
                    sendText(exchange, "{}", 201);
                } catch (IllegalArgumentException e) {
                    sendHasInteractions(exchange);
                }
            } else if ("DELETE".equals(method)) {
                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));
                    manager.deleteSubtaskById(id);
                } else {
                    manager.deleteSubtasks();
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
