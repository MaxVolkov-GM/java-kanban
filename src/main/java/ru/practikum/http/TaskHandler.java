package ru.practikum.http;

import com.google.gson.Gson;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Task;

import java.io.IOException;
import java.util.List;

public class TaskHandler extends BaseHttpHandler {

    public TaskHandler(TaskManager manager, Gson gson) {
        super(manager, gson);
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();

            if ("GET".equals(method)) {
                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));
                    Task task = manager.getTaskById(id);
                    if (task == null) sendNotFound(exchange);
                    else sendText(exchange, gson.toJson(task), 200);
                } else {
                    List<Task> tasks = manager.getAllTasks();
                    sendText(exchange, gson.toJson(tasks), 200);
                }

            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                if (body.isEmpty()) {
                    sendBadRequest(exchange);
                    return;
                }
                Task task = gson.fromJson(body, Task.class);
                if (task.getId() == 0) manager.createTask(task);
                else manager.updateTask(task);
                sendText(exchange, "{}", 201);

            } else if ("DELETE".equals(method)) {
                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));
                    manager.deleteTaskById(id);
                    sendText(exchange, "{}", 200);
                } else {
                    sendBadRequest(exchange);
                }

            } else {
                sendMethodNotAllowed(exchange);
            }

        } catch (Exception e) {
            sendServerError(exchange);
        }
    }
}
