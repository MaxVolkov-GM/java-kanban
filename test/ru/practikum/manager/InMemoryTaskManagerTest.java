package ru.practikum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import ru.practikum.model.*;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {
    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = Managers.getDefault();
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
    void shouldRemoveTaskFromHistoryWhenDeleted() {
        Task task = new Task("Task", "Description", Status.NEW);
        int taskId = manager.createTask(task);

        manager.getTaskById(taskId);
        assertEquals(1, manager.getHistory().size(), "Задача должна быть в истории");

        manager.deleteTaskById(taskId);
        assertTrue(manager.getHistory().isEmpty(), "История должна быть пустой после удаления задачи");
    }

    @Test
    void shouldRemoveEpicAndSubtasksFromHistoryWhenDeleted() {
        Epic epic = new Epic("Epic", "Description");
        int epicId = manager.createEpic(epic);
        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, epicId);
        int subtaskId = manager.createSubtask(subtask);

        manager.getEpicById(epicId);
        manager.getSubtaskById(subtaskId);
        assertEquals(2, manager.getHistory().size(), "Эпик и подзадача должны быть в истории");

        manager.deleteEpicById(epicId);
        assertTrue(manager.getHistory().isEmpty(), "История должна быть пустой после удаления эпика");
    }

    @Test
    void shouldRemoveSubtaskFromHistoryWhenDeleted() {
        Epic epic = new Epic("Epic", "Description");
        int epicId = manager.createEpic(epic);
        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, epicId);
        int subtaskId = manager.createSubtask(subtask);

        manager.getSubtaskById(subtaskId);
        assertEquals(1, manager.getHistory().size(), "Подзадача должна быть в истории");

        manager.deleteSubtaskById(subtaskId);
        assertTrue(manager.getHistory().isEmpty(), "История должна быть пустой после удаления подзадачи");
    }

    @Test
    void historyShouldNotContainDuplicates() {
        Task task = new Task("Task", "Description", Status.NEW);
        int taskId = manager.createTask(task);

        manager.getTaskById(taskId);
        manager.getTaskById(taskId);
        manager.getTaskById(taskId);

        assertEquals(1, manager.getHistory().size(), "История должна содержать только одну запись без дубликатов");
    }

    @Test
    void shouldMaintainHistoryOrder() {
        Task task1 = new Task("Task 1", "Description", Status.NEW);
        Task task2 = new Task("Task 2", "Description", Status.NEW);
        Task task3 = new Task("Task 3", "Description", Status.NEW);

        int taskId1 = manager.createTask(task1);
        int taskId2 = manager.createTask(task2);
        int taskId3 = manager.createTask(task3);

        manager.getTaskById(taskId1);
        manager.getTaskById(taskId2);
        manager.getTaskById(taskId3);

        assertEquals(3, manager.getHistory().size());
        assertEquals(taskId1, manager.getHistory().get(0).getId());
        assertEquals(taskId2, manager.getHistory().get(1).getId());
        assertEquals(taskId3, manager.getHistory().get(2).getId());
    }

    @Test
    void shouldUpdateHistoryOrderWhenTaskReaccessed() {
        Task task1 = new Task("Task 1", "Description", Status.NEW);
        Task task2 = new Task("Task 2", "Description", Status.NEW);
        Task task3 = new Task("Task 3", "Description", Status.NEW);

        int taskId1 = manager.createTask(task1);
        int taskId2 = manager.createTask(task2);
        int taskId3 = manager.createTask(task3);

        manager.getTaskById(taskId1);
        manager.getTaskById(taskId2);
        manager.getTaskById(taskId3);

        manager.getTaskById(taskId1);

        assertEquals(3, manager.getHistory().size());
        assertEquals(taskId2, manager.getHistory().get(0).getId());
        assertEquals(taskId3, manager.getHistory().get(1).getId());
        assertEquals(taskId1, manager.getHistory().get(2).getId());
    }

    @Test
    void epicStatusShouldBeUpdatedWhenSubtasksChange() {
        Epic epic = new Epic("Epic", "Description");
        int epicId = manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Subtask 1", "Description", Status.NEW, epicId);
        Subtask subtask2 = new Subtask("Subtask 2", "Description", Status.NEW, epicId);
        int subtaskId1 = manager.createSubtask(subtask1);
        int subtaskId2 = manager.createSubtask(subtask2);

        assertEquals(Status.NEW, manager.getEpicById(epicId).getStatus(), "Статус должен быть NEW");

        Subtask updatedSubtask = new Subtask("Updated", "Description", Status.DONE, epicId);
        updatedSubtask.setId(subtaskId1);
        manager.updateSubtask(updatedSubtask);

        assertEquals(Status.IN_PROGRESS, manager.getEpicById(epicId).getStatus(), "Статус должен быть IN_PROGRESS");

        Subtask updatedSubtask2 = new Subtask("Updated 2", "Description", Status.DONE, epicId);
        updatedSubtask2.setId(subtaskId2);
        manager.updateSubtask(updatedSubtask2);

        assertEquals(Status.DONE, manager.getEpicById(epicId).getStatus(), "Статус должен быть DONE");
    }

    @Test
    void shouldNotAllowSubtaskWithoutEpic() {
        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, 999);

        int result = manager.createSubtask(subtask);

        assertEquals(-1, result, "Создание подзадачи без эпика должно возвращать -1");
        assertTrue(manager.getAllSubtasks().isEmpty(), "Подзадача не должна быть создана");
    }

    @Test
    void shouldReturnEmptyListForNonExistentEpicSubtasks() {
        List<Subtask> subtasks = manager.getSubtasksByEpicId(999);
        assertTrue(subtasks.isEmpty(), "Должен возвращаться пустой список для несуществующего эпика");
    }
}