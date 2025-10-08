package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Subtask;

import java.io.IOException;

public class SubtaskHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public SubtaskHandler(TaskManager manager, Gson gson) {
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
                if (segments.length == 2 && segments[1].equals("subtasks")) {
                    sendText(exchange, gson.toJson(manager.getAllSubtasks()));
                } else if (segments.length == 3 && segments[1].equals("subtasks")) {
                    int id = Integer.parseInt(segments[2]);
                    Subtask subtask = manager.getSubtaskById(id);
                    if (subtask != null) {
                        sendText(exchange, gson.toJson(subtask));
                    } else {
                        sendText(exchange, "Subtask not found", 404);
                    }
                } else {
                    sendServerError(exchange);
                }
            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                Subtask subtask = gson.fromJson(body, Subtask.class);
                manager.createSubtask(subtask);
                sendText(exchange, "Subtask created", 201);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }
}
