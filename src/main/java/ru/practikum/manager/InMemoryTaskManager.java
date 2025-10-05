package ru.practikum.manager;

import ru.practikum.model.Epic;
import ru.practikum.model.Subtask;
import ru.practikum.model.Task;
import ru.practikum.model.Status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    // --- Оптимизированная проверка пересечений за O(1) ---
    private static final int INTERVAL_MINUTES = 15;
    private static final int MINUTES_PER_YEAR = 365 * 24 * 60;
    private static final int INTERVALS_PER_YEAR = MINUTES_PER_YEAR / INTERVAL_MINUTES;
    private final boolean[] schedule = new boolean[INTERVALS_PER_YEAR];

    // Базовая дата для расчета индексов (можно использовать любую фиксированную дату)
    private static final LocalDateTime BASE_DATE = LocalDateTime.of(2024, 1, 1, 0, 0);

    // Перевод времени в индекс массива
    private int timeToIndex(LocalDateTime time) {
        long minutesFromBase = ChronoUnit.MINUTES.between(BASE_DATE, time);
        return (int) (minutesFromBase / INTERVAL_MINUTES);
    }

    // Проверка, что интервалы свободны
    private boolean areIntervalsFree(int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex >= INTERVALS_PER_YEAR) {
            return false; // Выход за границы планирования
        }
        
        for (int i = startIndex; i < endIndex; i++) {
            if (schedule[i]) {
                return false;
            }
        }
        return true;
    }

    // Занять интервалы
    private void occupyIntervals(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            schedule[i] = true;
        }
    }

    // Освободить интервалы
    private void freeIntervals(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            schedule[i] = false;
        }
    }

    // Проверка пересечения с существующими задачами
    protected boolean hasTimeOverlapWithExisting(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return false;
        }

        LocalDateTime startTime = task.getStartTime();
        LocalDateTime endTime = task.getEndTime();
        
        int startIndex = timeToIndex(startTime);
        int endIndex = timeToIndex(endTime);
        
        // Проверяем границы
        if (startIndex < 0 || endIndex >= INTERVALS_PER_YEAR) {
            return true; // Выход за границы планирования = пересечение
        }
        
        return !areIntervalsFree(startIndex, endIndex);
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
            // Занимаем интервалы
            LocalDateTime startTime = task.getStartTime();
            LocalDateTime endTime = task.getEndTime();
            int startIndex = timeToIndex(startTime);
            int endIndex = timeToIndex(endTime);
            occupyIntervals(startIndex, endIndex);
        }

        return task.getId();
    }

    @Override
    public void updateTask(Task task) {
        if (task == null || !tasks.containsKey(task.getId())) return;

        Task oldTask = tasks.get(task.getId());
        if (oldTask.getStartTime() != null) {
            prioritizedTasks.remove(oldTask);
            // Освобождаем старые интервалы
            LocalDateTime oldStartTime = oldTask.getStartTime();
            LocalDateTime oldEndTime = oldTask.getEndTime();
            int oldStartIndex = timeToIndex(oldStartTime);
            int oldEndIndex = timeToIndex(oldEndTime);
            freeIntervals(oldStartIndex, oldEndIndex);
        }

        if (hasTimeOverlapWithExisting(task)) {
            // Возвращаем старую задачу
            if (oldTask.getStartTime() != null) {
                prioritizedTasks.add(oldTask);
                LocalDateTime oldStartTime = oldTask.getStartTime();
                LocalDateTime oldEndTime = oldTask.getEndTime();
                int oldStartIndex = timeToIndex(oldStartTime);
                int oldEndIndex = timeToIndex(oldEndTime);
                occupyIntervals(oldStartIndex, oldEndIndex);
            }
            throw new IllegalArgumentException("Задача пересекается по времени с существующей задачей");
        }

        tasks.put(task.getId(), task);

        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
            // Занимаем новые интервалы
            LocalDateTime startTime = task.getStartTime();
            LocalDateTime endTime = task.getEndTime();
            int startIndex = timeToIndex(startTime);
            int endIndex = timeToIndex(endTime);
            occupyIntervals(startIndex, endIndex);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            historyManager.remove(id);
            if (task.getStartTime() != null) {
                prioritizedTasks.remove(task);
                // Освобождаем интервалы
                LocalDateTime startTime = task.getStartTime();
                LocalDateTime endTime = task.getEndTime();
                int startIndex = timeToIndex(startTime);
                int endIndex = timeToIndex(endTime);
                freeIntervals(startIndex, endIndex);
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
                    // Освобождаем интервалы подзадачи
                    LocalDateTime startTime = subtask.getStartTime();
                    LocalDateTime endTime = subtask.getEndTime();
                    int startIndex = timeToIndex(startTime);
                    int endIndex = timeToIndex(endTime);
                    freeIntervals(startIndex, endIndex);
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
            // Занимаем интервалы
            LocalDateTime startTime = subtask.getStartTime();
            LocalDateTime endTime = subtask.getEndTime();
            int startIndex = timeToIndex(startTime);
            int endIndex = timeToIndex(endTime);
            occupyIntervals(startIndex, endIndex);
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
            // Освобождаем старые интервалы
            LocalDateTime oldStartTime = oldSubtask.getStartTime();
            LocalDateTime oldEndTime = oldSubtask.getEndTime();
            int oldStartIndex = timeToIndex(oldStartTime);
            int oldEndIndex = timeToIndex(oldEndTime);
            freeIntervals(oldStartIndex, oldEndIndex);
        }

        if (hasTimeOverlapWithExisting(subtask)) {
            // Возвращаем старую подзадачу
            if (oldSubtask.getStartTime() != null) {
                prioritizedTasks.add(oldSubtask);
                LocalDateTime oldStartTime = oldSubtask.getStartTime();
                LocalDateTime oldEndTime = oldSubtask.getEndTime();
                int oldStartIndex = timeToIndex(oldStartTime);
                int oldEndIndex = timeToIndex(oldEndTime);
                occupyIntervals(oldStartIndex, oldEndIndex);
            }
            throw new IllegalArgumentException("Подзадача пересекается по времени с существующей задачей");
        }

        subtasks.put(subtask.getId(), subtask);
        updateEpicStatus(epic);
        updateEpicTime(epic);

        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
            // Занимаем новые интервалы
            LocalDateTime startTime = subtask.getStartTime();
            LocalDateTime endTime = subtask.getEndTime();
            int startIndex = timeToIndex(startTime);
            int endIndex = timeToIndex(endTime);
            occupyIntervals(startIndex, endIndex);
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
                // Освобождаем интервалы
                LocalDateTime startTime = subtask.getStartTime();
                LocalDateTime endTime = subtask.getEndTime();
                int startIndex = timeToIndex(startTime);
                int endIndex = timeToIndex(endTime);
                freeIntervals(startIndex, endIndex);
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
            if (t.getStartTime() != null) {
                prioritizedTasks.remove(t);
                LocalDateTime startTime = t.getStartTime();
                LocalDateTime endTime = t.getEndTime();
                int startIndex = timeToIndex(startTime);
                int endIndex = timeToIndex(endTime);
                freeIntervals(startIndex, endIndex);
            }
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
            if (s.getStartTime() != null) {
                prioritizedTasks.remove(s);
                LocalDateTime startTime = s.getStartTime();
                LocalDateTime endTime = s.getEndTime();
                int startIndex = timeToIndex(startTime);
                int endIndex = timeToIndex(endTime);
                freeIntervals(startIndex, endIndex);
            }
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
                    if (s.getStartTime() != null) {
                        prioritizedTasks.remove(s);
                        LocalDateTime startTime = s.getStartTime();
                        LocalDateTime endTime = s.getEndTime();
                        int startIndex = timeToIndex(startTime);
                        int endIndex = timeToIndex(endTime);
                        freeIntervals(startIndex, endIndex);
                    }
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
