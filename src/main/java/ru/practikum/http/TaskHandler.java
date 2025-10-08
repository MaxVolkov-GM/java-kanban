package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Task;

import java.io.IOException;

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
            if ("GET".equals(method)) {
                String[] segments = path.split("/");
                if (segments.length == 2 && segments[1].equals("tasks")) {
                    sendText(exchange, gson.toJson(manager.getAllTasks()));
                } else if (segments.length == 3 && segments[1].equals("tasks")) {
                    int id = Integer.parseInt(segments[2]);
                    Task task = manager.getTaskById(id);
                    if (task != null) {
                        sendText(exchange, gson.toJson(task));
                    } else {
                        sendText(exchange, "Task not found", 404);
                    }
                } else {
                    sendServerError(exchange);
                }
            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                Task task = gson.fromJson(body, Task.class);
                manager.createTask(task);
                sendText(exchange, "Task created", 201);
            } else {
                sendServerError(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }
}
