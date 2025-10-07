package ru.practikum.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practikum.manager.InMemoryTaskManager;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Subtask;
import ru.practikum.model.Epic;
import ru.practikum.model.Status;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpTaskManagerSubtasksTest {

    private TaskManager manager;
    private HttpTaskServer server;
    private Gson gson;

    @BeforeAll
    public void setupServer() throws IOException {
        manager = new InMemoryTaskManager();
        server = new HttpTaskServer(manager);
        server.start();
        gson = HttpTaskServer.getGson();
    }

    @AfterAll
    public void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clearManager() {
        manager.deleteTasks();
        manager.deleteSubtasks();
        manager.deleteEpics();
    }

    @Test
    public void testCreateAndGetSubtask() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Description");
        int epicId = manager.createEpic(epic);

        Subtask subtask = new Subtask("Subtask 1", "Description", Status.NEW, epicId,
                LocalDateTime.now(), Duration.ofMinutes(20));

        String json = gson.toJson(subtask);

        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create("http://localhost:8080/subtasks");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());
    }
}
