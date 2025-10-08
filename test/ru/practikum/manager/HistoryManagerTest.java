package ru.practikum.manager;

import org.junit.jupiter.api.Test;
import ru.practikum.model.Task;
import ru.practikum.model.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryManagerTest {

    @Test
    void add() {
        HistoryManager historyManager = new InMemoryHistoryManager();
        Task task = new Task("Test addNewTask", "Test addNewTask description", Status.NEW);
        task.setId(1);

        historyManager.add(task);
        final List<Task> history = historyManager.getHistory();
        assertNotNull(history, "История не пустая.");
        assertEquals(1, history.size(), "История не пустая.");
    }

    @Test
    void addDuplicate() {
        HistoryManager historyManager = new InMemoryHistoryManager();
        Task task = new Task("Test addNewTask", "Test addNewTask description", Status.NEW);
        task.setId(1);

        historyManager.add(task);
        historyManager.add(task);
        final List<Task> history = historyManager.getHistory();
        assertNotNull(history, "История не пустая.");
        assertEquals(1, history.size(), "История не пустая.");
    }

    @Test
    void removeFromStart() {
        HistoryManager historyManager = new InMemoryHistoryManager();
        Task task1 = new Task("Test addNewTask1", "Test addNewTask description1", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Test addNewTask2", "Test addNewTask description2", Status.NEW);
        task2.setId(2);
        Task task3 = new Task("Test addNewTask3", "Test addNewTask description3", Status.NEW);
        task3.setId(3);

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        historyManager.remove(1);
        final List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size(), "Неверное количество задач в истории.");
        assertEquals(task2, history.get(0), "Первая задача не совпадает.");
        assertEquals(task3, history.get(1), "Вторая задача не совпадает.");
    }

    @Test
    void removeFromMiddle() {
        HistoryManager historyManager = new InMemoryHistoryManager();
        Task task1 = new Task("Test addNewTask1", "Test addNewTask description1", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Test addNewTask2", "Test addNewTask description2", Status.NEW);
        task2.setId(2);
        Task task3 = new Task("Test addNewTask3", "Test addNewTask description3", Status.NEW);
        task3.setId(3);

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        historyManager.remove(2);
        final List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size(), "Неверное количество задач в истории.");
        assertEquals(task1, history.get(0), "Первая задача не совпадает.");
        assertEquals(task3, history.get(1), "Вторая задача не совпадает.");
    }

    @Test
    void removeFromEnd() {
        HistoryManager historyManager = new InMemoryHistoryManager();
        Task task1 = new Task("Test addNewTask1", "Test addNewTask description1", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Test addNewTask2", "Test addNewTask description2", Status.NEW);
        task2.setId(2);
        Task task3 = new Task("Test addNewTask3", "Test addNewTask description3", Status.NEW);
        task3.setId(3);

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        historyManager.remove(3);
        final List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size(), "Неверное количество задач в истории.");
        assertEquals(task1, history.get(0), "Первая задача не совпадает.");
        assertEquals(task2, history.get(1), "Вторая задача не совпадает.");
    }
}
