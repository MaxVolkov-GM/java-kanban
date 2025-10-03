package ru.practikum.manager.manager;

import org.junit.jupiter.api.Test;
import ru.practikum.manager.HistoryManager;
import ru.practikum.manager.Managers;
import ru.practikum.manager.TaskManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagersTest {
    @Test
    void shouldReturnInitializedTaskManager() {
        TaskManager manager = Managers.getDefault();
        assertNotNull(manager);
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    void shouldReturnInitializedHistoryManager() {
        HistoryManager history = Managers.getDefaultHistory();
        assertNotNull(history);
        assertTrue(history.getHistory().isEmpty());
    }
}