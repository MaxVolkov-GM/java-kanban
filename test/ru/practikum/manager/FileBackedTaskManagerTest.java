package ru.practikum.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.practikum.model.Epic;
import ru.practikum.model.Status;
import ru.practikum.model.Subtask;
import ru.practikum.model.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {
    
    @TempDir
    Path tempDir;
    
    @Override
    protected FileBackedTaskManager createManager() {
        try {
            File file = Files.createTempFile(tempDir, "test", ".csv").toFile();
            return new FileBackedTaskManager(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp file", e);
        }
    }
    
    @Test
    void testSaveAndLoadEmptyFile() throws IOException {
        File file = Files.createTempFile(tempDir, "empty", ".csv").toFile();
        FileBackedTaskManager manager1 = new FileBackedTaskManager(file);
        
        manager1.save();
        
        FileBackedTaskManager manager2 = FileBackedTaskManager.loadFromFile(file);
        
        assertTrue(manager2.getAllTasks().isEmpty(), "Список задач должен быть пустым");
        assertTrue(manager2.getAllEpics().isEmpty(), "Список эпиков должен быть пустым");
        assertTrue(manager2.getAllSubtasks().isEmpty(), "Список подзадач должен быть пустым");
    }
    
    @Test
    void testSaveAndLoadWithTasks() throws IOException {
        File file = Files.createTempFile(tempDir, "tasks", ".csv").toFile();
        FileBackedTaskManager manager1 = new FileBackedTaskManager(file);
        
        Task task = new Task("Test task", "Description", Status.IN_PROGRESS,
                           LocalDateTime.of(2024, 1, 1, 10, 0), Duration.ofHours(1));
        manager1.createTask(task);
        
        Epic epic = new Epic("Test epic", "Epic description");
        int epicId = manager1.createEpic(epic);
        
        Subtask subtask = new Subtask("Test subtask", "Subtask description", Status.DONE, epicId,
                                    LocalDateTime.of(2024, 1, 1, 12, 0), Duration.ofMinutes(30));
        manager1.createSubtask(subtask);
        
        FileBackedTaskManager manager2 = FileBackedTaskManager.loadFromFile(file);
        
        List<Task> tasks = manager2.getAllTasks();
        List<Epic> epics = manager2.getAllEpics();
        List<Subtask> subtasks = manager2.getAllSubtasks();
        
        assertEquals(1, tasks.size(), "Должна быть 1 задача");
        assertEquals(1, epics.size(), "Должен быть 1 эпик");
        assertEquals(1, subtasks.size(), "Должна быть 1 подзадача");
        
        Task loadedTask = tasks.get(0);
        assertEquals("Test task", loadedTask.getName(), "Название задачи не совпадает");
        assertEquals(Status.IN_PROGRESS, loadedTask.getStatus(), "Статус задачи не совпадает");
        assertNotNull(loadedTask.getStartTime(), "Время начала задачи должно сохраниться");
        assertNotNull(loadedTask.getDuration(), "Продолжительность задачи должна сохраниться");
        
        Subtask loadedSubtask = subtasks.get(0);
        assertEquals(epicId, loadedSubtask.getEpicId(), "ID эпика подзадачи не совпадает");
    }
    
    @Test
    void testLoadFromNonExistentFile() {
        File file = new File("non_existent_file.csv");
        FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(file);
        
        assertNotNull(manager, "Менеджер должен создаться даже для несуществующего файла");
        assertTrue(manager.getAllTasks().isEmpty(), "Список задач должен быть пустым");
    }
    
    @Test
    void testSaveAndLoadEpicTimeCalculation() throws IOException {
        File file = Files.createTempFile(tempDir, "epic_time", ".csv").toFile();
        FileBackedTaskManager manager1 = new FileBackedTaskManager(file);
        
        Epic epic = new Epic("Test epic", "Epic description");
        int epicId = manager1.createEpic(epic);
        
        LocalDateTime startTime1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        Duration duration1 = Duration.ofHours(1);
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", Status.NEW, epicId, startTime1, duration1);
        manager1.createSubtask(subtask1);
        
        LocalDateTime startTime2 = LocalDateTime.of(2024, 1, 1, 12, 0);
        Duration duration2 = Duration.ofHours(2);
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", Status.NEW, epicId, startTime2, duration2);
        manager1.createSubtask(subtask2);
        
        FileBackedTaskManager manager2 = FileBackedTaskManager.loadFromFile(file);
        
        Epic loadedEpic = manager2.getEpicById(epicId);
        assertNotNull(loadedEpic, "Эпик должен быть загружен");
        assertEquals(startTime1, loadedEpic.getStartTime(), "Время начала эпика должно сохраниться");
        assertEquals(Duration.ofHours(3), loadedEpic.getDuration(), "Продолжительность эпика должна сохраниться");
        assertEquals(startTime2.plus(duration2), loadedEpic.getEndTime(), "Время окончания эпика должно сохраниться");
    }
}
