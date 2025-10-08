package ru.practikum.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void tasksWithSameIdShouldBeEqual() {
        Task task1 = new Task("Task 1", "Description 1", Status.NEW);
        task1.setId(1);

        Task task2 = new Task("Task 2", "Description 2", Status.DONE);
        task2.setId(1);

        assertEquals(task1, task2, "Две задачи с одинаковым ID должны быть равны");
        assertEquals(task1.hashCode(), task2.hashCode(), "Хэш-коды должны совпадать");
    }

    @Test
    void taskCopyWithConstructor() {
        Task original = new Task("Original", "Description", Status.IN_PROGRESS);
        original.setId(1);

        Task copy = new Task(original);

        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original.getStatus(), copy.getStatus());
    }

    @Test
    void taskShouldNotEqualDifferentObject() {
        Task task = new Task("Task", "Description", Status.NEW);
        task.setId(1);

        assertNotEquals("Not a task", task);
    }
}