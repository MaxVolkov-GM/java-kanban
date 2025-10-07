package ru.practikum.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Epic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
            String[] segments = path.split("/");

            switch (method) {
                case "GET":
                    if (segments.length == 2) {
                        List<Epic> epics = manager.getAllEpics();
                        sendText(exchange, gson.toJson(epics));
                    } else if (segments.length == 3) {
                        int id = Integer.parseInt(segments[2]);
                        Epic epic = manager.getEpicById(id);
                        if (epic != null) sendText(exchange, gson.toJson(epic));
                        else sendText(exchange, "{\"error\":\"Эпик не найден\"}", 404);
                    } else if (segments.length == 4 && "subtasks".equals(segments[3])) {
                        int epicId = Integer.parseInt(segments[2]);
                        List<?> subtasks = manager.getSubtasksByEpicId(epicId);
                        sendText(exchange, gson.toJson(subtasks));
                    }
                    break;

                case "POST":
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    Epic epic = gson.fromJson(body, Epic.class);

                    if (epic.getId() == 0) {
                        int newId = manager.createEpic(epic);
                        sendText(exchange, "{\"id\":" + newId + "}", 201);
                    } else {
                        manager.updateEpic(epic);
                        sendText(exchange, "{\"id\":" + epic.getId() + "}", 201);
                    }
                    break;

                case "DELETE":
                    if (segments.length == 3) {
                        int id = Integer.parseInt(segments[2]);
                        manager.deleteEpicById(id);
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
