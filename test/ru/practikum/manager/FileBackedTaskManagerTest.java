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

        // Создаем задачи с временем
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        Task task = new Task("Test Task", "Description", Status.NEW, startTime, Duration.ofHours(1));
        Epic epic = new Epic("Test Epic", "Description");
        Subtask subtask = new Subtask("Test Subtask", "Description", Status.NEW, 2, 
                                     startTime.plusHours(2), Duration.ofMinutes(30));

        int taskId = manager.createTask(task);
        int epicId = manager.createEpic(epic);
        subtask = new Subtask(subtask.getName(), subtask.getDescription(), subtask.getStatus(), 
                             epicId, subtask.getStartTime(), subtask.getDuration());
        int subtaskId = manager.createSubtask(subtask);

        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);

        // Проверяем восстановление задач с константами
        Task loadedTask = loadedManager.getTaskById(taskId);
        assertNotNull(loadedTask, "Задача не должна быть null");
        assertEquals("Test Task", loadedTask.getName(), "Имя задачи должно совпадать");
        assertEquals("Description", loadedTask.getDescription(), "Описание задачи должно совпадать");
        assertEquals(Status.NEW, loadedTask.getStatus(), "Статус задачи должен совпадать");
        assertEquals(taskId, loadedTask.getId(), "ID задачи должен совпадать");
        assertEquals(startTime, loadedTask.getStartTime(), "Время начала должно совпадать");
        assertEquals(Duration.ofHours(1), loadedTask.getDuration(), "Продолжительность должна совпадать");

        Epic loadedEpic = loadedManager.getEpicById(epicId);
        assertNotNull(loadedEpic, "Эпик не должен быть null");
        assertEquals("Test Epic", loadedEpic.getName(), "Имя эпика должно совпадать");
        assertEquals("Description", loadedEpic.getDescription(), "Описание эпика должно совпадать");

        // Проверяем восстановление времени эпика
        assertNotNull(loadedEpic.getStartTime(), "Время начала эпика должно быть восстановлено");
        assertNotNull(loadedEpic.getEndTime(), "Время окончания эпика должно быть восстановлено");
        assertEquals(Duration.ofMinutes(30), loadedEpic.getDuration(), "Продолжительность эпика должна совпадать");

        // Проверяем восстановление prioritizedTasks
        List<Task> prioritized = loadedManager.getPrioritizedTasks();
        assertEquals(2, prioritized.size(), "Должны быть восстановлены 2 задачи с временем");
        assertEquals("Test Task", prioritized.get(0).getName(), "Первой должна быть ранняя задача");
        assertEquals("Test Subtask", prioritized.get(1).getName(), "Второй должна быть поздняя задача");
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

        // Загружаем и проверяем восстановление времени эпика
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        Epic loadedEpic = loadedManager.getEpicById(epicId);

        assertEquals(startTime1, loadedEpic.getStartTime(), "Время начала эпика должно быть восстановлено");
        assertEquals(startTime2.plusHours(2), loadedEpic.getEndTime(), "Время окончания эпика должно быть восстановлено");
        assertEquals(Duration.ofHours(3), loadedEpic.getDuration(), "Продолжительность эпика должна быть восстановлена");
    }

    @Test
    void testLoadFromNonExistentFile() throws IOException {
        File file = tempDir.resolve("nonexistent.csv").toFile();
        FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(file);

        assertTrue(manager.getAllTasks().isEmpty(), "Менеджер должен быть пустым для несуществующего файла");
        assertTrue(manager.getAllEpics().isEmpty(), "Менеджер должен быть пустым для несуществующего файла");
        assertTrue(manager.getAllSubtasks().isEmpty(), "Менеджер должен быть пустым для несуществующего файла");
    }

    @Test
    void testSaveAndLoadEmptyFile() throws IOException {
        File file = tempDir.resolve("empty.csv").toFile();
        Files.writeString(file.toPath(), "id,type,name,status,description,startTime,duration,epic\n");

        FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(file);

        assertTrue(manager.getAllTasks().isEmpty(), "Менеджер должен быть пустым для пустого файла");
        assertTrue(manager.getAllEpics().isEmpty(), "Менеджер должен быть пустым для пустого файла");
        assertTrue(manager.getAllSubtasks().isEmpty(), "Менеджер должен быть пустым для пустого файла");
    }

    @Test
    void testPrioritizedTasksRestoration() throws IOException {
        File file = tempDir.resolve("prioritized.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        // Создаем задачи в разном порядке
        LocalDateTime earlyTime = LocalDateTime.of(2024, 1, 1, 9, 0);
        LocalDateTime lateTime = LocalDateTime.of(2024, 1, 1, 11, 0);
        
        Task lateTask = new Task("Late Task", "Description", Status.NEW, lateTime, Duration.ofHours(1));
        Task earlyTask = new Task("Early Task", "Description", Status.NEW, earlyTime, Duration.ofHours(1));
        
        manager.createTask(lateTask);
        manager.createTask(earlyTask);

        // Загружаем и проверяем что prioritizedTasks восстановлен корректно
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        List<Task> prioritized = loadedManager.getPrioritizedTasks();

        assertEquals(2, prioritized.size(), "Должны быть восстановлены 2 задачи");
        assertEquals("Early Task", prioritized.get(0).getName(), "Первой должна быть ранняя задача");
        assertEquals("Late Task", prioritized.get(1).getName(), "Второй должна быть поздняя задача");
    }
}
