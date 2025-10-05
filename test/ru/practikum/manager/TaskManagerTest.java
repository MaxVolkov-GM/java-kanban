package ru.practikum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practikum.model.Epic;
import ru.practikum.model.Status;
import ru.practikum.model.Subtask;
import ru.practikum.model.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

abstract class TaskManagerTest<T extends TaskManager> {
    protected T manager;
    
    protected abstract T createManager();
    
    @BeforeEach
    void setUp() {
        manager = createManager();
    }
    
    @Test
    void testCreateAndGetTask() {
        Task task = new Task("Test task", "Test description", Status.NEW);
        int taskId = manager.createTask(task);
        
        Task savedTask = manager.getTaskById(taskId);
        assertNotNull(savedTask, "Задача не найдена");
        assertEquals(task.getName(), savedTask.getName(), "Названия задач не совпадают");
        assertEquals(task.getDescription(), savedTask.getDescription(), "Описания задач не совпадают");
        assertEquals(task.getStatus(), savedTask.getStatus(), "Статусы задач не совпадают");
    }
    
    @Test
    void testCreateAndGetEpic() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        
        Epic savedEpic = manager.getEpicById(epicId);
        assertNotNull(savedEpic, "Эпик не найден");
        assertEquals(epic.getName(), savedEpic.getName(), "Названия эпиков не совпадают");
        assertEquals(Status.NEW, savedEpic.getStatus(), "Статус нового эпика должен быть NEW");
    }
    
    @Test
    void testCreateAndGetSubtask() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        
        Subtask subtask = new Subtask("Test subtask", "Test subtask description", Status.NEW, epicId);
        int subtaskId = manager.createSubtask(subtask);
        
        Subtask savedSubtask = manager.getSubtaskById(subtaskId);
        assertNotNull(savedSubtask, "Подзадача не найдена");
        assertEquals(subtask.getName(), savedSubtask.getName(), "Названия подзадач не совпадают");
        assertEquals(epicId, savedSubtask.getEpicId(), "ID эпика не совпадает");
    }
    
    @Test
    void testEpicStatusCalculation() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", Status.NEW, epicId);
        manager.createSubtask(subtask1);
        assertEquals(Status.NEW, manager.getEpicById(epicId).getStatus(), "Эпик должен быть NEW");
        
        manager.deleteSubtasks();
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", Status.DONE, epicId);
        Subtask subtask3 = new Subtask("Subtask 3", "Description 3", Status.DONE, epicId);
        manager.createSubtask(subtask2);
        manager.createSubtask(subtask3);
        assertEquals(Status.DONE, manager.getEpicById(epicId).getStatus(), "Эпик должен быть DONE");
        
        manager.deleteSubtasks();
        Subtask subtask4 = new Subtask("Subtask 4", "Description 4", Status.NEW, epicId);
        Subtask subtask5 = new Subtask("Subtask 5", "Description 5", Status.DONE, epicId);
        manager.createSubtask(subtask4);
        manager.createSubtask(subtask5);
        assertEquals(Status.IN_PROGRESS, manager.getEpicById(epicId).getStatus(), "Эпик должен быть IN_PROGRESS");
        
        manager.deleteSubtasks();
        Subtask subtask6 = new Subtask("Subtask 6", "Description 6", Status.IN_PROGRESS, epicId);
        manager.createSubtask(subtask6);
        assertEquals(Status.IN_PROGRESS, manager.getEpicById(epicId).getStatus(), "Эпик должен быть IN_PROGRESS");
    }
    
    @Test
    void testTaskWithTime() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        Duration duration = Duration.ofHours(2);
        
        Task task = new Task("Test task", "Test description", Status.NEW, startTime, duration);
        int taskId = manager.createTask(task);
        
        Task savedTask = manager.getTaskById(taskId);
        assertEquals(startTime, savedTask.getStartTime(), "Время начала не совпадает");
        assertEquals(duration, savedTask.getDuration(), "Продолжительность не совпадает");
        assertEquals(startTime.plus(duration), savedTask.getEndTime(), "Время окончания не совпадает");
    }
    
    @Test
    void testEpicTimeCalculation() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        
        LocalDateTime startTime1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        Duration duration1 = Duration.ofHours(1);
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", Status.NEW, epicId, startTime1, duration1);
        manager.createSubtask(subtask1);
        
        LocalDateTime startTime2 = LocalDateTime.of(2024, 1, 1, 12, 0);
        Duration duration2 = Duration.ofHours(2);
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", Status.NEW, epicId, startTime2, duration2);
        manager.createSubtask(subtask2);
        
        Epic savedEpic = manager.getEpicById(epicId);
        assertEquals(startTime1, savedEpic.getStartTime(), "Время начала эпика должно быть самым ранним");
        assertEquals(Duration.ofHours(3), savedEpic.getDuration(), "Продолжительность эпика должна быть суммой");
        assertEquals(startTime2.plus(duration2), savedEpic.getEndTime(), "Время окончания эпика должно быть самым поздним");
    }
    
    @Test
    void testEpicTimeWithNoSubtasks() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        
        Epic savedEpic = manager.getEpicById(epicId);
        assertNull(savedEpic.getStartTime(), "Время начала должно быть null для эпика без подзадач");
        assertEquals(Duration.ZERO, savedEpic.getDuration(), "Продолжительность должна быть 0 для эпика без подзадач");
        assertNull(savedEpic.getEndTime(), "Время окончания должно быть null для эпика без подзадач");
    }
    
    @Test
    void testPrioritizedTasks() {
        LocalDateTime earlyTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime lateTime = LocalDateTime.of(2024, 1, 1, 12, 0);
        
        Task task1 = new Task("Late task", "Description", Status.NEW, lateTime, Duration.ofHours(1));
        Task task2 = new Task("Early task", "Description", Status.NEW, earlyTime, Duration.ofHours(1));
        Task taskWithoutTime = new Task("No time task", "Description", Status.NEW);
        
        manager.createTask(task1);
        manager.createTask(task2);
        manager.createTask(taskWithoutTime);
        
        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(2, prioritized.size(), "В приоритетном списке должны быть только задачи со временем");
        assertEquals("Early task", prioritized.get(0).getName(), "Первой должна быть ранняя задача");
        assertEquals("Late task", prioritized.get(1).getName(), "Второй должна быть поздняя задача");
    }
    
    @Test
    void testTimeOverlap() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        Duration duration = Duration.ofHours(2);
        
        Task task1 = new Task("Task 1", "Description", Status.NEW, startTime, duration);
        manager.createTask(task1);
        
        Task task2 = new Task("Task 2", "Description", Status.NEW, 
                             startTime.plusHours(1), Duration.ofHours(1));
        
        assertThrows(IllegalArgumentException.class, () -> manager.createTask(task2),
                    "Должно быть исключение при пересечении по времени");
        
        Task task3 = new Task("Task 3", "Description", Status.NEW,
                             startTime.plusHours(3), Duration.ofHours(1));
        
        assertDoesNotThrow(() -> manager.createTask(task3),
                         "Не должно быть исключения для непересекающихся задач");
    }
    
    @Test
    void testTimeOverlapWithSubtask() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        Duration duration = Duration.ofHours(2);
        
        Subtask subtask1 = new Subtask("Subtask 1", "Description", Status.NEW, epicId, startTime, duration);
        manager.createSubtask(subtask1);
        
        Subtask subtask2 = new Subtask("Subtask 2", "Description", Status.NEW, epicId,
                                     startTime.plusHours(1), Duration.ofHours(1));
        
        assertThrows(IllegalArgumentException.class, () -> manager.createSubtask(subtask2),
                    "Должно быть исключение при пересечении по времени для подзадач");
    }
    
    @Test
    void testHistory() {
        Task task = new Task("Test task", "Test description", Status.NEW);
        int taskId = manager.createTask(task);
        
        manager.getTaskById(taskId);
        List<Task> history = manager.getHistory();
        
        assertEquals(1, history.size(), "История должна содержать 1 задачу");
        assertEquals(taskId, history.get(0).getId(), "ID задачи в истории не совпадает");
    }
    
    @Test
    void testDeleteTasks() {
        Task task1 = new Task("Task 1", "Description 1", Status.NEW);
        Task task2 = new Task("Task 2", "Description 2", Status.NEW);
        manager.createTask(task1);
        manager.createTask(task2);
        
        manager.deleteTasks();
        assertEquals(0, manager.getAllTasks().size(), "Все задачи должны быть удалены");
    }
    
    @Test
    void testDeleteEpics() {
        Epic epic1 = new Epic("Epic 1", "Description 1");
        Epic epic2 = new Epic("Epic 2", "Description 2");
        manager.createEpic(epic1);
        manager.createEpic(epic2);
        
        manager.deleteEpics();
        assertEquals(0, manager.getAllEpics().size(), "Все эпики должны быть удалены");
    }
}
