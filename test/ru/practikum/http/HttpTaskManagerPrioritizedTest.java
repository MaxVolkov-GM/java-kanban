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

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpTaskManagerPrioritizedTest {

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
    public void testPrioritizedTasks() throws IOException, InterruptedException {
        Task task1 = new Task("Task 1", "Desc", Status.NEW, LocalDateTime.now(), Duration.ofMinutes(20));
        Task task2 = new Task("Task 2", "Desc", Status.NEW, LocalDateTime.now().plusMinutes(30), Duration.ofMinutes(30));
        manager.createTask(task1);
        manager.createTask(task2);

        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create("http://localhost:8080/prioritized");
        HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }
}
