package ru.practikum.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.practikum.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoadWithTasks() throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        LocalDateTime startTimeTask = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime startTimeSubtask = startTimeTask.plusHours(2);

        Task task = new Task("Test Task", "Description", Status.NEW, startTimeTask, Duration.ofHours(1));
        Epic epic = new Epic("Test Epic", "Description");

        int taskId = manager.createTask(task);
        int epicId = manager.createEpic(epic);

        Subtask subtask = new Subtask("Test Subtask", "Description", Status.NEW, 0,
                startTimeSubtask, Duration.ofMinutes(30));
        subtask = new Subtask(subtask.getName(), subtask.getDescription(), subtask.getStatus(),
                epicId, subtask.getStartTime(), subtask.getDuration());
        int subtaskId = manager.createSubtask(subtask);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        Task loadedTask = loadedManager.getTaskById(taskId);
        assertNotNull(loadedTask);
        assertEquals(task.getName(), loadedTask.getName());
        assertEquals(task.getDescription(), loadedTask.getDescription());
        assertEquals(task.getStatus(), loadedTask.getStatus());
        assertEquals(taskId, loadedTask.getId());
        assertEquals(startTimeTask, loadedTask.getStartTime());
        assertEquals(Duration.ofHours(1), loadedTask.getDuration());

        Epic loadedEpic = loadedManager.getEpicById(epicId);
        assertNotNull(loadedEpic);
        assertEquals(epic.getName(), loadedEpic.getName());
        assertEquals(epic.getDescription(), loadedEpic.getDescription());
        assertNotNull(loadedEpic.getStartTime());
        assertNotNull(loadedEpic.getEndTime());
        assertEquals(Duration.ofMinutes(30), loadedEpic.getDuration());

        Subtask loadedSubtask = loadedManager.getSubtaskById(subtaskId);
        assertNotNull(loadedSubtask);
        assertEquals(subtask.getName(), loadedSubtask.getName());
        assertEquals(subtask.getEpicId(), loadedSubtask.getEpicId());

        List<Task> prioritized = loadedManager.getPrioritizedTasks();
        assertEquals(2, prioritized.size());
        assertEquals("Test Task", prioritized.get(0).getName());
        assertEquals("Test Subtask", prioritized.get(1).getName());
    }

    @Test
    void testSaveAndLoadEpicTimeCalculation() throws IOException {
        File file = tempDir.resolve("test_epic.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Epic epic = new Epic("Test Epic", "Description");
        int epicId = manager.createEpic(epic);

        LocalDateTime startTime1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime startTime2 = LocalDateTime.of(2024, 1, 1, 12, 0);

        manager.createSubtask(new Subtask("Subtask 1", "Description", Status.NEW, epicId,
                startTime1, Duration.ofHours(1)));
        manager.createSubtask(new Subtask("Subtask 2", "Description", Status.NEW, epicId,
                startTime2, Duration.ofHours(2)));

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        Epic loadedEpic = loadedManager.getEpicById(epicId);

        assertEquals(startTime1, loadedEpic.getStartTime());
        assertEquals(startTime2.plusHours(2), loadedEpic.getEndTime());
        assertEquals(Duration.ofHours(3), loadedEpic.getDuration());
    }

    @Test
    void testLoadFromNonExistentFile() throws IOException {
        File file = tempDir.resolve("nonexistent.csv").toFile();
        FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(file);

        assertTrue(manager.getAllTasks().isEmpty());
        assertTrue(manager.getAllEpics().isEmpty());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    void testSaveAndLoadEmptyFile() throws IOException {
        File file = tempDir.resolve("empty.csv").toFile();
        Files.writeString(file.toPath(), "id,type,name,status,description,startTime,duration,epic\n");

        FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(file);

        assertTrue(manager.getAllTasks().isEmpty());
        assertTrue(manager.getAllEpics().isEmpty());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    void testPrioritizedTasksRestoration() throws IOException {
        File file = tempDir.resolve("prioritized.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        LocalDateTime earlyTime = LocalDateTime.of(2024, 1, 1, 9, 0);
        LocalDateTime lateTime = LocalDateTime.of(2024, 1, 1, 11, 0);

        Task lateTask = new Task("Late Task", "Description", Status.NEW, lateTime, Duration.ofHours(1));
        Task earlyTask = new Task("Early Task", "Description", Status.NEW, earlyTime, Duration.ofHours(1));

        manager.createTask(lateTask);
        manager.createTask(earlyTask);

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        List<Task> prioritized = loadedManager.getPrioritizedTasks();

        assertEquals(2, prioritized.size());
        assertEquals("Early Task", prioritized.get(0).getName());
        assertEquals("Late Task", prioritized.get(1).getName());
    }
}
