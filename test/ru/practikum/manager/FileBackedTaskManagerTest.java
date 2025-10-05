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

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {

    @Test
    void saveAndLoadEmptyFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        
        manager.save();

        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        
        assertEquals(0, loadedManager.getAllTasks().size(), "Количество задач должно быть 0");
        assertEquals(0, loadedManager.getAllEpics().size(), "Количество эпиков должно быть 0");
        assertEquals(0, loadedManager.getAllSubtasks().size(), "Количество подзадач должно быть 0");
    }

    @Test
    void saveAndLoadWithTasks(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        
        // Создаем задачи
        Task task = new Task("Task 1", "Description 1", Status.NEW);
        int taskId = manager.createTask(task);
        
        Epic epic = new Epic("Epic 1", "Epic Description 1");
        int epicId = manager.createEpic(epic);
        
        Subtask subtask = new Subtask("Subtask 1", "Subtask Description 1", Status.DONE, epicId);
        int subtaskId = manager.createSubtask(subtask);
        
        // Загружаем из файла
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        
        assertEquals(1, loadedManager.getAllTasks().size(), "Количество задач должно быть 1");
        assertEquals(1, loadedManager.getAllEpics().size(), "Количество эпиков должно быть 1");
        assertEquals(1, loadedManager.getAllSubtasks().size(), "Количество подзадач должно быть 1");
        
        // Проверяем, что все поля задач восстановились корректно
        Task loadedTask = loadedManager.getTaskById(taskId);
        assertNotNull(loadedTask, "Задача должна быть не null");
        assertEquals(task.getName(), loadedTask.getName(), "Имя задачи должно совпадать");
        assertEquals(task.getDescription(), loadedTask.getDescription(), "Описание задачи должно совпадать");
        assertEquals(task.getStatus(), loadedTask.getStatus(), "Статус задачи должен совпадать");
        assertEquals(task.getId(), loadedTask.getId(), "ID задачи должен совпадать");
        
        Epic loadedEpic = loadedManager.getEpicById(epicId);
        assertNotNull(loadedEpic, "Эпик должен быть не null");
        assertEquals(epic.getName(), loadedEpic.getName(), "Имя эпика должно совпадать");
        assertEquals(epic.getDescription(), loadedEpic.getDescription(), "Описание эпика должно совпадать");
        assertEquals(epic.getId(), loadedEpic.getId(), "ID эпика должен совпадать");
        assertEquals(Status.DONE, loadedEpic.getStatus(), "Статус эпика должен быть DONE");
        assertEquals(1, loadedEpic.getSubtaskIds().size(), "Эпик должен содержать одну подзадачу");
        assertTrue(loadedEpic.getSubtaskIds().contains(subtaskId), "Эпик должен содержать ID подзадачи");
        
        Subtask loadedSubtask = loadedManager.getSubtaskById(subtaskId);
        assertNotNull(loadedSubtask, "Подзадача должна быть не null");
        assertEquals(subtask.getName(), loadedSubtask.getName(), "Имя подзадачи должно совпадать");
        assertEquals(subtask.getDescription(), loadedSubtask.getDescription(), "Описание подзадачи должно совпадать");
        assertEquals(subtask.getStatus(), loadedSubtask.getStatus(), "Статус подзадачи должен совпадать");
        assertEquals(subtask.getId(), loadedSubtask.getId(), "ID подзадачи должен совпадать");
        assertEquals(subtask.getEpicId(), loadedSubtask.getEpicId(), "Epic ID подзадачи должен совпадать");
    }

    @Test
    void fileFormat(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        
        Task task = new Task("Test Task", "Test Description", Status.DONE);
        manager.createTask(task);
        
        String content = Files.readString(file.toPath());
        String[] lines = content.split("\n");
        
        assertEquals("id,type,name,status,description,epic", lines[0], "Заголовок CSV должен совпадать");
        assertTrue(lines[1].startsWith("1,TASK,Test Task,DONE,Test Description,"), 
                   "Формат строки задачи должен быть корректным");
    }

    @Test
    void loadWithEpicAndSubtasks(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        
        Epic epic = new Epic("Test Epic", "Epic Description");
        int epicId = manager.createEpic(epic);
        
        Subtask subtask = new Subtask("Test Subtask", "Subtask Description", Status.IN_PROGRESS, epicId);
        manager.createSubtask(subtask);
        
        FileBackedTaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        
        Epic loadedEpic = loadedManager.getEpicById(epicId);
        assertNotNull(loadedEpic, "Эпик должен быть не null");
        assertEquals(1, loadedEpic.getSubtaskIds().size(), "Эпик должен содержать одну подзадачу");
        assertEquals(Status.IN_PROGRESS, loadedEpic.getStatus(), "Статус эпика должен быть IN_PROGRESS");
        
        Subtask loadedSubtask = loadedManager.getSubtaskById(subtask.getId());
        assertNotNull(loadedSubtask, "Подзадача должна быть не null");
        assertEquals(epicId, loadedSubtask.getEpicId(), "Epic ID подзадачи должен совпадать");
    }
}
