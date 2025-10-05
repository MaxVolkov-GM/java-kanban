package ru.practikum.manager;

import ru.practikum.model.Epic;
import ru.practikum.model.Subtask;
import ru.practikum.model.Task;
import ru.practikum.model.Status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected int sequence = 0;
    protected final HistoryManager historyManager = Managers.getDefaultHistory();
    protected final Set<Task> prioritizedTasks = new TreeSet<>(
            Comparator.comparing(Task::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Task::getId)
    );

    // Простая и надежная проверка пересечений
    protected boolean hasTimeOverlapWithExisting(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getEndTime() == null) {
            return false;
        }

        for (Task existingTask : prioritizedTasks) {
            if (existingTask.getId() != newTask.getId() && isOverlapping(newTask, existingTask)) {
                return true;
            }
        }
        return false;
    }

    // Проверка пересечения двух задач по времени
    protected boolean isOverlapping(Task task1, Task task2) {
        if (task1.getStartTime() == null || task1.getEndTime() == null ||
            task2.getStartTime() == null || task2.getEndTime() == null) {
            return false;
        }

        return task1.getStartTime().isBefore(task2.getEndTime()) &&
               task1.getEndTime().isAfter(task2.getStartTime());
    }

    // --- CRUD для задач ---
    @Override
    public int createTask(Task task) {
        if (task == null) return -1;

        if (hasTimeOverlapWithExisting(task)) {
            throw new IllegalArgumentException("Задача пересекается по времени с существующей задачей");
        }

        task.setId(++sequence);
        tasks.put(task.getId(), task);

        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }

        return task.getId();
    }

    @Override
    public void updateTask(Task task) {
        if (task == null || !tasks.containsKey(task.getId())) return;

        Task oldTask = tasks.get(task.getId());
        if (oldTask.getStartTime() != null) {
            prioritizedTasks.remove(oldTask);
        }

        if (hasTimeOverlapWithExisting(task)) {
            // возвращаем старую задачу
            if (oldTask.getStartTime() != null) {
                prioritizedTasks.add(oldTask);
            }
            throw new IllegalArgumentException("Задача пересекается по времени с существующей задачей");
        }

        tasks.put(task.getId(), task);

        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            historyManager.remove(id);
            if (task.getStartTime() != null) {
                prioritizedTasks.remove(task);
            }
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    // --- CRUD для эпиков ---
    @Override
    public int createEpic(Epic epic) {
        if (epic == null) return -1;
        epic.setId(++sequence);
        epics.put(epic.getId(), epic);
        updateEpicTime(epic);
        return epic.getId();
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic == null || !epics.containsKey(epic.getId())) return;

        Epic savedEpic = epics.get(epic.getId());
        savedEpic.setName(epic.getName());
        savedEpic.setDescription(epic.getDescription());
        updateEpicTime(savedEpic);
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (Integer subtaskId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.remove(subtaskId);
                if (subtask != null && subtask.getStartTime() != null) {
                    prioritizedTasks.remove(subtask);
                }
                historyManager.remove(subtaskId);
            }
            historyManager.remove(id);
        }
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) historyManager.add(epic);
        return epic;
    }

    // --- CRUD для подзадач ---
    @Override
    public int createSubtask(Subtask subtask) {
        if (subtask == null) return -1;
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) return -1;

        if (hasTimeOverlapWithExisting(subtask)) {
            throw new IllegalArgumentException("Подзадача пересекается по времени с существующей задачей");
        }

        subtask.setId(++sequence);
        subtasks.put(subtask.getId(), subtask);
        epic.addSubtaskId(subtask.getId());
        updateEpicStatus(epic);
        updateEpicTime(epic);

        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
        }

        return subtask.getId();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask == null || !subtasks.containsKey(subtask.getId())) return;

        Subtask oldSubtask = subtasks.get(subtask.getId());
        Epic epic = epics.get(oldSubtask.getEpicId());
        if (epic == null) return;

        if (oldSubtask.getStartTime() != null) {
            prioritizedTasks.remove(oldSubtask);
        }

        if (hasTimeOverlapWithExisting(subtask)) {
            if (oldSubtask.getStartTime() != null) {
                prioritizedTasks.add(oldSubtask);
            }
            throw new IllegalArgumentException("Подзадача пересекается по времени с существующей задачей");
        }

        subtasks.put(subtask.getId(), subtask);
        updateEpicStatus(epic);
        updateEpicTime(epic);

        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                updateEpicStatus(epic);
                updateEpicTime(epic);
            }
            historyManager.remove(id);
            if (subtask.getStartTime() != null) {
                prioritizedTasks.remove(subtask);
            }
        }
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) historyManager.add(subtask);
        return subtask;
    }

    @Override
    public List<Subtask> getSubtasksByEpicId(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return new ArrayList<>();
        List<Subtask> result = new ArrayList<>();
        for (Integer subId : epic.getSubtaskIds()) {
            Subtask sub = subtasks.get(subId);
            if (sub != null) result.add(sub);
        }
        return result;
    }

    // --- История ---
    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    // --- Удаление всех элементов ---
    @Override
    public void deleteTasks() {
        for (Task t : tasks.values()) {
            if (t.getStartTime() != null) prioritizedTasks.remove(t);
            historyManager.remove(t.getId());
        }
        tasks.clear();
    }

    @Override
    public void deleteSubtasks() {
        for (Subtask s : subtasks.values()) {
            Epic epic = epics.get(s.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(s.getId());
                updateEpicStatus(epic);
                updateEpicTime(epic);
            }
            if (s.getStartTime() != null) prioritizedTasks.remove(s);
            historyManager.remove(s.getId());
        }
        subtasks.clear();
    }

    @Override
    public void deleteEpics() {
        for (Epic e : epics.values()) {
            for (Integer subId : e.getSubtaskIds()) {
                Subtask s = subtasks.remove(subId);
                if (s != null) {
                    if (s.getStartTime() != null) prioritizedTasks.remove(s);
                    historyManager.remove(subId);
                }
            }
            historyManager.remove(e.getId());
        }
        epics.clear();
    }

    // --- Приоритетные задачи ---
    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    // --- Вспомогательные методы эпиков ---
    protected void updateEpicStatus(Epic epic) {
        if (epic.getSubtaskIds().isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (Integer subId : epic.getSubtaskIds()) {
            Subtask sub = subtasks.get(subId);
            if (sub == null) continue;
            if (sub.getStatus() != Status.NEW) allNew = false;
            if (sub.getStatus() != Status.DONE) allDone = false;
        }

        if (allNew) epic.setStatus(Status.NEW);
        else if (allDone) epic.setStatus(Status.DONE);
        else epic.setStatus(Status.IN_PROGRESS);
    }

    protected void updateEpicTime(Epic epic) {
        if (epic.getSubtaskIds().isEmpty()) {
            epic.setCalculatedStartTime(null);
            epic.setCalculatedDuration(Duration.ZERO);
            epic.setEndTime(null);
            return;
        }

        LocalDateTime earliest = null;
        LocalDateTime latest = null;
        Duration total = Duration.ZERO;

        for (Integer subId : epic.getSubtaskIds()) {
            Subtask s = subtasks.get(subId);
            if (s == null || s.getStartTime() == null) continue;

            if (earliest == null || s.getStartTime().isBefore(earliest)) earliest = s.getStartTime();
            LocalDateTime end = s.getEndTime();
            if (end != null && (latest == null || end.isAfter(latest))) latest = end;

            if (s.getDuration() != null) total = total.plus(s.getDuration());
        }

        epic.setCalculatedStartTime(earliest);
        epic.setCalculatedDuration(total);
        epic.setEndTime(latest);
    }
}
