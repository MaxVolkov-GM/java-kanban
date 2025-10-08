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
    public void testCreateGetUpdateDeleteSubtask() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Desc");
        int epicId = manager.createEpic(epic);

        Subtask subtask = new Subtask("Subtask 1", "Desc", Status.NEW, epicId, LocalDateTime.now(), Duration.ofMinutes(20));
        String json = gson.toJson(subtask);

        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create("http://localhost:8080/subtasks");

        HttpRequest postRequest = HttpRequest.newBuilder().uri(uri).POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postResponse.statusCode());

        HttpRequest getRequest = HttpRequest.newBuilder().uri(uri).GET().build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());

        Subtask[] subtasks = gson.fromJson(getResponse.body(), Subtask[].class);
        subtasks[0].setName("Updated Subtask");
        String updateJson = gson.toJson(subtasks[0]);
        HttpRequest updateRequest = HttpRequest.newBuilder().uri(uri).POST(HttpRequest.BodyPublishers.ofString(updateJson)).build();
        HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, updateResponse.statusCode());

        HttpRequest deleteRequest = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/subtasks?id=" + subtasks[0].getId())).DELETE().build();
        HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, deleteResponse.statusCode());
    }
}
