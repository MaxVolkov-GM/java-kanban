package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Task;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TaskHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public TaskHandler(TaskManager manager, Gson gson) {
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
                        List<Task> tasks = manager.getAllTasks();
                        sendText(exchange, gson.toJson(tasks));
                    } else if (segments.length == 3) {
                        int id = Integer.parseInt(segments[2]);
                        Task task = manager.getTaskById(id);
                        if (task != null) sendText(exchange, gson.toJson(task));
                        else sendText(exchange, "{\"error\":\"Задача не найдена\"}", 404);
                    }
                    break;

                case "POST":
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Task task = gson.fromJson(body, Task.class);

                    if (task.getId() == 0) {
                        try {
                            int newId = manager.createTask(task);
                            sendText(exchange, "{\"id\":" + newId + "}", 201);
                        } catch (IllegalArgumentException e) {
                            sendText(exchange, "{\"error\":\"Задача пересекается по времени с другой задачей\"}", 400);
                        }
                    } else {
                        try {
                            manager.updateTask(task);
                            sendText(exchange, "{\"id\":" + task.getId() + "}", 201);
                        } catch (IllegalArgumentException e) {
                            sendText(exchange, "{\"error\":\"Задача пересекается по времени с другой задачей\"}", 400);
                        }
                    }
                    break;

                case "DELETE":
                    if (segments.length == 3) {
                        int id = Integer.parseInt(segments[2]);
                        manager.deleteTaskById(id);
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
