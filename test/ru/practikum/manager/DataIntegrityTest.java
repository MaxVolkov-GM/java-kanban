package ru.practikum.manager;

import org.junit.jupiter.api.Test;
import ru.practikum.model.Epic;
import ru.practikum.model.Subtask;

import static org.junit.jupiter.api.Assertions.*;

class DataIntegrityTest {



    @Test
    void shouldRemoveSubtaskIdsWhenEpicDeleted() {
        TaskManager manager = Managers.getDefault();

        Epic epic = new Epic("Epic", "Description");
        int epicId = manager.createEpic(epic);

        Subtask subtask = new Subtask("Subtask", "Description", ru.practikum.model.Status.NEW, epicId);
        int subtaskId = manager.createSubtask(subtask);

        manager.deleteEpicById(epicId);

        assertNull(manager.getSubtaskById(subtaskId));
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    void shouldUpdateEpicStatusWhenSubtaskDeleted() {
        TaskManager manager = Managers.getDefault();

        Epic epic = new Epic("Epic", "Description");
        int epicId = manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Subtask 1", "Description", ru.practikum.model.Status.DONE, epicId);
        Subtask subtask2 = new Subtask("Subtask 2", "Description", ru.practikum.model.Status.DONE, epicId);
        int subtaskId1 = manager.createSubtask(subtask1);
        int subtaskId2 = manager.createSubtask(subtask2);

        assertEquals(ru.practikum.model.Status.DONE, manager.getEpicById(epicId).getStatus());

        manager.deleteSubtaskById(subtaskId1);

        assertEquals(ru.practikum.model.Status.DONE, manager.getEpicById(epicId).getStatus());

        manager.deleteSubtaskById(subtaskId2);

        assertEquals(ru.practikum.model.Status.NEW, manager.getEpicById(epicId).getStatus());
    }

    @Test
    void shouldHandleEmptyEpicStatusCorrectly() {
        TaskManager manager = Managers.getDefault();

        Epic epic = new Epic("Epic", "Description");
        int epicId = manager.createEpic(epic);

        assertEquals(ru.practikum.model.Status.NEW, manager.getEpicById(epicId).getStatus());
    }
}