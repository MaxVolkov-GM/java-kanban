package ru.practikum.manager;

import org.junit.jupiter.api.Test;
import ru.practikum.model.Task;
import ru.practikum.model.Status;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeGridOptimizationTest {

    @Test
    void testGridBasedOverlapChecking() {
        InMemoryTaskManager manager = new InMemoryTaskManager();

        // Задачи в пределах текущего года (должны использовать оптимизированную проверку через сетку)
        LocalDateTime baseTime = LocalDateTime.now().withHour(10).withMinute(0);
        
        Task task1 = new Task("Task 1", "Description", Status.NEW, baseTime, Duration.ofMinutes(30));
        Task task2 = new Task("Task 2", "Description", Status.NEW, 
                             baseTime.plusMinutes(15), Duration.ofMinutes(30));

        // Первая задача создается успешно
        int task1Id = manager.createTask(task1);
        assertTrue(task1Id > 0, "Первая задача должна создаться успешно");

        // Вторая задача пересекается по времени - должно быть исключение
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(task2),
                "Задачи пересекаются по 15-минутной сетке");
        assertTrue(exception.getMessage().contains("пересекается"));
    }

    @Test
    void testNonOverlappingTasksInGrid() {
        InMemoryTaskManager manager = new InMemoryTaskManager();

        LocalDateTime baseTime = LocalDateTime.now().withHour(10).withMinute(0);
        
        Task task1 = new Task("Task 1", "Description", Status.NEW,
                baseTime, Duration.ofMinutes(30));
        Task task2 = new Task("Task 2", "Description", Status.NEW,
                baseTime.plusMinutes(45), Duration.ofMinutes(30));

        // Обе задачи должны создаться успешно (не пересекаются)
        int task1Id = manager.createTask(task1);
        int task2Id = manager.createTask(task2);
        
        assertTrue(task1Id > 0, "Первая задача должна создаться успешно");
        assertTrue(task2Id > 0, "Вторая задача должна создаться успешно");
        assertEquals(2, manager.getPrioritizedTasks().size(), "Должны быть 2 задачи в приоритетном списке");
    }

    @Test
    void testTasksOutsideGridRange() {
        InMemoryTaskManager manager = new InMemoryTaskManager();

        // Задачи за пределами текущего года (должны использовать обычную проверку через Stream API)
        LocalDateTime futureTime = LocalDateTime.now().plusYears(2).withHour(10).withMinute(0);
        
        Task task1 = new Task("Future Task 1", "Description", Status.NEW,
                futureTime, Duration.ofMinutes(30));
        Task task2 = new Task("Future Task 2", "Description", Status.NEW,
                futureTime.plusMinutes(15), Duration.ofMinutes(30));

        // Первая задача создается успешно
        int task1Id = manager.createTask(task1);
        assertTrue(task1Id > 0, "Задача вне сетки должна создаться успешно");

        // Вторая задача пересекается - должно быть исключение (проверка через Stream API)
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(task2),
                "Задачи вне сетки также проверяются на пересечение через Stream API");
        assertTrue(exception.getMessage().contains("пересекается"));
    }

    @Test
    void testGridBoundaryConditions() {
        InMemoryTaskManager manager = new InMemoryTaskManager();

        // Граничные условия: задачи точно по 15-минутным интервалам
        LocalDateTime time1 = LocalDateTime.now().withHour(10).withMinute(0);
        LocalDateTime time2 = LocalDateTime.now().withHour(10).withMinute(15);
        LocalDateTime time3 = LocalDateTime.now().withHour(10).withMinute(30);

        Task task1 = new Task("Task 1", "Description", Status.NEW,
                time1, Duration.ofMinutes(15)); // 10:00-10:15
        Task task2 = new Task("Task 2", "Description", Status.NEW,
                time2, Duration.ofMinutes(15)); // 10:15-10:30 - не пересекается
        Task task3 = new Task("Task 3", "Description", Status.NEW,
                time2, Duration.ofMinutes(16)); // 10:15-10:31 - пересекается с task2

        // task1 и task2 не пересекаются
        int task1Id = manager.createTask(task1);
        int task2Id = manager.createTask(task2);
        assertTrue(task1Id > 0 && task2Id > 0, "Задачи на границах интервалов не должны пересекаться");

        // task3 пересекается с task2
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> manager.createTask(task3),
                "Задача, захватывающая следующий интервал, должна считаться пересекающейся");
        assertTrue(exception.getMessage().contains("пересекается"));
    }

    @Test
    void testHybridApproach() {
        InMemoryTaskManager manager = new InMemoryTaskManager();

        // Смешанный тест: задачи в сетке и вне сетки
        LocalDateTime inGridTime = LocalDateTime.now().withHour(10).withMinute(0);
        LocalDateTime outOfGridTime = LocalDateTime.now().plusYears(2).withHour(10).withMinute(0);
        
        Task inGridTask = new Task("In Grid Task", "Description", Status.NEW,
                inGridTime, Duration.ofMinutes(30));
        Task outOfGridTask = new Task("Out of Grid Task", "Description", Status.NEW,
                outOfGridTime, Duration.ofMinutes(30));

        // Обе задачи должны создаться успешно (разные временные диапазоны)
        int task1Id = manager.createTask(inGridTask);
        int task2Id = manager.createTask(outOfGridTask);
        
        assertTrue(task1Id > 0, "Задача в сетке должна создаться успешно");
        assertTrue(task2Id > 0, "Задача вне сетки должна создаться успешно");
        assertEquals(2, manager.getPrioritizedTasks().size(), "Должны быть 2 задачи в приоритетном списке");
    }
}
