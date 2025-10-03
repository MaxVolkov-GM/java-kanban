package ru.practikum.manager.manager;

import org.junit.jupiter.api.Test;
import ru.practikum.manager.HistoryManager;
import ru.practikum.manager.Managers;
import ru.practikum.model.Status;
import ru.practikum.model.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryManagerTest {
    @Test
    void shouldPreserveOriginalTaskState() {
        HistoryManager history = Managers.getDefaultHistory();

        Task original = new Task("Тестовая задача", "Описание", Status.NEW);
        original.setId(1);

        history.add(original);

        original.setStatus(Status.DONE);
        original.setName("Измененная задача");

        Task historical = history.getHistory().get(0);

        assertEquals(Status.NEW, historical.getStatus(), "Статус должен остаться NEW");
        assertEquals("Тестовая задача", historical.getName(), "Название должно остаться оригинальным");
    }

    @Test
    void shouldRemoveDuplicatesWhenTaskAddedMultipleTimes() {
        HistoryManager history = Managers.getDefaultHistory();

        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setId(2);

        history.add(task1);
        history.add(task2);
        history.add(task1);

        assertEquals(2, history.getHistory().size(), "Должно быть 2 уникальные задачи");
        assertEquals(task2, history.getHistory().get(0), "Первой должна быть task2");
        assertEquals(task1, history.getHistory().get(1), "Последней должна быть task1");
    }

    @Test
    void shouldRemoveTaskFromHistory() {
        HistoryManager history = Managers.getDefaultHistory();

        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setId(2);
        Task task3 = new Task("Задача 3", "Описание", Status.NEW);
        task3.setId(3);

        history.add(task1);
        history.add(task2);
        history.add(task3);

        history.remove(2);

        assertEquals(2, history.getHistory().size());
        assertEquals(task1, history.getHistory().get(0));
        assertEquals(task3, history.getHistory().get(1));
    }

    @Test
    void shouldRemoveFromBeginning() {
        HistoryManager history = Managers.getDefaultHistory();

        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setId(2);

        history.add(task1);
        history.add(task2);

        history.remove(1);

        assertEquals(1, history.getHistory().size());
        assertEquals(task2, history.getHistory().get(0));
    }

    @Test
    void shouldRemoveFromEnd() {
        HistoryManager history = Managers.getDefaultHistory();

        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setId(2);

        history.add(task1);
        history.add(task2);

        history.remove(2);

        assertEquals(1, history.getHistory().size());
        assertEquals(task1, history.getHistory().get(0));
    }

    @Test
    void shouldHandleEmptyHistory() {
        HistoryManager history = Managers.getDefaultHistory();

        assertTrue(history.getHistory().isEmpty(), "История должна быть пустой");

        history.remove(999);
        assertTrue(history.getHistory().isEmpty());
    }

    @Test
    void shouldNotAddNullTask() {
        HistoryManager history = Managers.getDefaultHistory();

        history.add(null);

        assertTrue(history.getHistory().isEmpty(), "История должна остаться пустой при добавлении null");
    }

    @Test
    void shouldMaintainOrderAfterMultipleOperations() {
        HistoryManager history = Managers.getDefaultHistory();

        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setId(2);
        Task task3 = new Task("Задача 3", "Описание", Status.NEW);
        task3.setId(3);

        history.add(task1);
        history.add(task2);
        history.add(task1);
        history.add(task3);
        history.remove(2);
        history.add(task2);

        assertEquals(3, history.getHistory().size());
        assertEquals(task1, history.getHistory().get(0));
        assertEquals(task3, history.getHistory().get(1));
        assertEquals(task2, history.getHistory().get(2));
    }

    @Test
    void shouldHandleSingleElementList() {
        HistoryManager history = Managers.getDefaultHistory();

        Task task = new Task("Задача", "Описание", Status.NEW);
        task.setId(1);

        history.add(task);
        assertEquals(1, history.getHistory().size());

        history.remove(1);
        assertTrue(history.getHistory().isEmpty());
    }
}