package ru.practikum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practikum.model.*;

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
    void shouldAddAndFindDifferentTaskTypes() {
        int taskId = manager.createTask(new Task("Task", "Description", Status.NEW));
        int epicId = manager.createEpic(new Epic("Epic", "Description"));
        int subtaskId = manager.createSubtask(new Subtask("Subtask", "Description", Status.NEW, epicId));

        assertNotNull(manager.getTaskById(taskId));
        assertNotNull(manager.getEpicById(epicId));
        assertNotNull(manager.getSubtaskById(subtaskId));

        assertEquals(1, manager.getAllTasks().size());
        assertEquals(1, manager.getAllEpics().size());
        assertEquals(1, manager.getAllSubtasks().size());
    }

    @Test
    void taskShouldRemainUnchangedAfterAdding() {
        Task original = new Task("Original", "Description", Status.NEW);
        original.setId(1);

        manager.createTask(original);
        Task saved = manager.getTaskById(1);

        assertEquals(original.getId(), saved.getId());
        assertEquals(original.getName(), saved.getName());
        assertEquals(original.getDescription(), saved.getDescription());
        assertEquals(original.getStatus(), saved.getStatus());
    }

    @Test
    void epicStatusShouldBeNewWhenNoSubtasks() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);

        assertEquals(Status.NEW, manager.getEpicById(epicId).getStatus(), "Эпик должен быть NEW");
    }

    @Test
    void epicStatusShouldBeNewWhenAllSubtasksNew() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        manager.createSubtask(new Subtask("Subtask 1", "Description", Status.NEW, epicId));
        manager.createSubtask(new Subtask("Subtask 2", "Description", Status.NEW, epicId));

        assertEquals(Status.NEW, manager.getEpicById(epicId).getStatus(), "Эпик должен быть NEW");
    }

    @Test
    void epicStatusShouldBeDoneWhenAllSubtasksDone() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        manager.createSubtask(new Subtask("Subtask 1", "Description", Status.DONE, epicId));
        manager.createSubtask(new Subtask("Subtask 2", "Description", Status.DONE, epicId));

        assertEquals(Status.DONE, manager.getEpicById(epicId).getStatus(), "Эпик должен быть DONE");
    }

    @Test
    void epicStatusShouldBeInProgressWhenSubtasksMixed() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        manager.createSubtask(new Subtask("Subtask 1", "Description", Status.NEW, epicId));
        manager.createSubtask(new Subtask("Subtask 2", "Description", Status.DONE, epicId));

        assertEquals(Status.IN_PROGRESS, manager.getEpicById(epicId).getStatus(), "Эпик должен быть IN_PROGRESS");
    }

    @Test
    void epicStatusShouldBeInProgressWhenSubtasksInProgress() {
        Epic epic = new Epic("Test epic", "Test epic description");
        int epicId = manager.createEpic(epic);
        manager.createSubtask(new Subtask("Subtask 1", "Description", Status.IN_PROGRESS, epicId));
        manager.createSubtask(new Subtask("Subtask 2", "Description", Status.IN_PROGRESS, epicId));

        assertEquals(Status.IN_PROGRESS, manager.getEpicById(epicId).getStatus(), "Эпик должен быть IN_PROGRESS");
    }

    @Test
    void taskWithTimeShouldSaveTimeCorrectly() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        Duration duration = Duration.ofHours(2);
        
        Task task = new Task("Task with time", "Description", Status.NEW, startTime, duration);
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
        LocalDateTime startTime2 = LocalDateTime.of(2024, 1, 1, 12, 0);
        Duration duration1 = Duration.ofHours(1);
        Duration duration2 = Duration.ofHours(2);
        
        manager.createSubtask(new Subtask("Subtask 1", "Description", Status.NEW, epicId, startTime1, duration1));
        manager.createSubtask(new Subtask("Subtask 2", "Description", Status.NEW, epicId, startTime2, duration2));
        
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
                    "Должно быть исключение при пересечении по времени подзадач");
    }

    @Test
    void shouldSaveAndRestoreHistory() {
        Task task = new Task("Task", "Description", Status.NEW);
        int taskId = manager.createTask(task);
        
        manager.getTaskById(taskId);
        List<Task> history = manager.getHistory();
        
        assertEquals(1, history.size(), "История должна содержать 1 задачу");
        assertEquals(taskId, history.get(0).getId(), "ID задачи в истории не совпадает");
    }

    @Test
    void shouldDeleteAllTasks() {
        manager.createTask(new Task("Task 1", "Description", Status.NEW));
        manager.createTask(new Task("Task 2", "Description", Status.NEW));
        
        manager.deleteTasks();
        assertEquals(0, manager.getAllTasks().size(), "Все задачи должны быть удалены");
    }

    @Test
    void shouldDeleteAllEpics() {
        manager.createEpic(new Epic("Epic 1", "Description"));
        manager.createEpic(new Epic("Epic 2", "Description"));
        
        manager.deleteEpics();
        assertEquals(0, manager.getAllEpics().size(), "Все эпики должны быть удалены");
    }

    @Test
    void shouldDeleteAllSubtasks() {
        Epic epic = new Epic("Epic", "Description");
        int epicId = manager.createEpic(epic);
        
        manager.createSubtask(new Subtask("Subtask 1", "Description", Status.NEW, epicId));
        manager.createSubtask(new Subtask("Subtask 2", "Description", Status.NEW, epicId));
        
        manager.deleteSubtasks();
        assertEquals(0, manager.getAllSubtasks().size(), "Все подзадачи должны быть удалены");
    }
}
