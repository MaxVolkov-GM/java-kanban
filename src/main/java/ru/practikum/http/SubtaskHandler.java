package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Subtask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
            String[] segments = path.split("/");

            switch (method) {
                case "GET":
                    if (segments.length == 2) {
                        List<Subtask> subtasks = manager.getAllSubtasks();
                        sendText(exchange, gson.toJson(subtasks));
                    } else if (segments.length == 3) {
                        int id = Integer.parseInt(segments[2]);
                        Subtask subtask = manager.getSubtaskById(id);
                        if (subtask != null) sendText(exchange, gson.toJson(subtask));
                        else sendText(exchange, "{\"error\":\"Подзадача не найдена\"}", 404);
                    }
                    break;

                case "POST":
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Subtask subtask = gson.fromJson(body, Subtask.class);

                    if (subtask.getId() == 0) {
                        try {
                            int newId = manager.createSubtask(subtask);
                            sendText(exchange, "{\"id\":" + newId + "}", 201);
                        } catch (IllegalArgumentException e) {
                            sendText(exchange, "{\"error\":\"Подзадача пересекается по времени с другой задачей\"}", 400);
                        }
                    } else {
                        try {
                            manager.updateSubtask(subtask);
                            sendText(exchange, "{\"id\":" + subtask.getId() + "}", 201);
                        } catch (IllegalArgumentException e) {
                            sendText(exchange, "{\"error\":\"Подзадача пересекается по времени с другой задачей\"}", 400);
                        }
                    }
                    break;

                case "DELETE":
                    if (segments.length == 3) {
                        int id = Integer.parseInt(segments[2]);
                        manager.deleteSubtaskById(id);
                        sendText(exchange, "{\"deleted\":" + id + "}");
                    }
                    break;

                default:
                    sendText(exchange, "{\"error\":\"Метод не поддерживается\"}", 405);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendText(exchange, "{\"error\":\"Внутренняя ошибка сервера\"}", 500);
        }
    }
}
