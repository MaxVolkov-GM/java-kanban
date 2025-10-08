package ru.practikum.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practikum.manager.InMemoryTaskManager;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Status;
import ru.practikum.model.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpTaskManagerTasksTest {

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
    public void testCreateAndGetTask() throws IOException, InterruptedException {
        Task task = new Task("Task 1", "Desc", Status.NEW, LocalDateTime.now(), Duration.ofMinutes(20));
        String json = gson.toJson(task);

        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create("http://localhost:8080/tasks");
        HttpRequest request = HttpRequest.newBuilder().uri(uri).POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        HttpRequest getRequest = HttpRequest.newBuilder().uri(uri).GET().build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());

        Task[] tasksFromServer = gson.fromJson(getResponse.body(), Task[].class);
        assertEquals(1, tasksFromServer.length);
        assertEquals(task.getName(), tasksFromServer[0].getName());
    }

    @Test
    public void testTaskNotFound() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create("http://localhost:8080/tasks?id=999");
        HttpRequest getRequest = HttpRequest.newBuilder().uri(uri).GET().build();
        HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}
