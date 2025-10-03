package ru.practikum.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubtaskTest {

    @Test
    void subtaskCopyWithConstructor() {
        Subtask original = new Subtask("Original", "Description", Status.NEW, 1);
        original.setId(1);

        Subtask copy = new Subtask(original);

        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original.getStatus(), copy.getStatus());
        assertEquals(original.getEpicId(), copy.getEpicId());
    }

    @Test
    void subtasksWithSameIdShouldBeEqual() {
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", Status.NEW, 1);
        subtask1.setId(1);

        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", Status.DONE, 2);
        subtask2.setId(1);

        assertEquals(subtask1, subtask2, "Две подзадачи с одинаковым ID должны быть равны");
        assertEquals(subtask1.hashCode(), subtask2.hashCode(), "Хэш-коды должны совпадать");
    }
}