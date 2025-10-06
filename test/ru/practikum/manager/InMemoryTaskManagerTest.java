package ru.practikum.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {

    @Override
    protected InMemoryTaskManager createManager() {
        return new InMemoryTaskManager();
    }

    // Здесь могут быть специфичные тесты для InMemoryTaskManager
    // Основные тесты наследуются из TaskManagerTest
    
    @Test
    void testInMemorySpecificFunctionality() {
        // Пример специфичного теста для InMemoryTaskManager
        assertNotNull(manager);
    }
}
