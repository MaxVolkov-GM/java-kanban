package ru.practikum.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpicTest {

    @Test
    void epicsWithSameIdShouldBeEqual() {
        Epic epic1 = new Epic("Epic 1", "Description 1");
        epic1.setId(1);

        Epic epic2 = new Epic("Epic 2", "Description 2");
        epic2.setId(1);

        assertEquals(epic1, epic2, "Два эпика с одинаковым ID должны быть равны");
        assertEquals(epic1.hashCode(), epic2.hashCode(), "Хэш-коды должны совпадать");
    }

    @Test
    void differentTaskTypesWithSameIdShouldNotBeEqual() {
        Epic epic = new Epic("Epic", "Description");
        epic.setId(1);

        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, 1);
        subtask.setId(1);

        Task task = new Task("Task", "Description", Status.NEW);
        task.setId(1);

        assertNotEquals(epic, subtask, "Эпик и подзадача с одинаковым ID не должны быть равны");
        assertNotEquals(epic, task, "Эпик и обычная задача с одинаковым ID не должны быть равны");
        assertNotEquals(subtask, task, "Подзадача и обычная задача с одинаковым ID не должны быть равны");
    }

    @Test
    void epicShouldNotEqualNull() {
        Epic epic = new Epic("Epic", "Description");
        epic.setId(1);
        assertNotEquals(null, epic, "Эпик не должен быть равен null");
    }

    @Test
    void epicShouldBeEqualToItself() {
        Epic epic = new Epic("Epic", "Description");
        epic.setId(1);
        assertEquals(epic, epic, "Эпик должен быть равен самому себе");
    }

    @Test
    void shouldManageSubtaskIdsCorrectly() {
        Epic epic = new Epic("Epic", "Description");

        epic.addSubtaskId(1);
        epic.addSubtaskId(2);
        epic.addSubtaskId(1); // Дубликат

        assertEquals(2, epic.getSubtaskIds().size(), "Должно быть 2 уникальных подзадачи");
        assertTrue(epic.getSubtaskIds().contains(1));
        assertTrue(epic.getSubtaskIds().contains(2));

        epic.removeSubtaskId(1);
        assertEquals(1, epic.getSubtaskIds().size());
        assertFalse(epic.getSubtaskIds().contains(1));
    }
}