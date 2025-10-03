package ru.practikum.manager;

import ru.practikum.model.Epic;
import ru.practikum.model.Subtask;
import ru.practikum.model.Task;

import java.util.List;

public interface TaskManager {
    List<Task> getAllTasks();
    Task getTaskById(int id);
    int createTask(Task task);
    void updateTask(Task task);
    void deleteTaskById(int id);

    List<Epic> getAllEpics();
    Epic getEpicById(int id);
    int createEpic(Epic epic);
    void updateEpic(Epic epic);
    void deleteEpicById(int id);

    List<Subtask> getAllSubtasks();
    Subtask getSubtaskById(int id);
    int createSubtask(Subtask subtask);
    void updateSubtask(Subtask subtask);
    void deleteSubtaskById(int id);

    List<Subtask> getSubtasksByEpicId(int epicId);
    List<Task> getHistory();
}