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
            Comparator.comparing(Task::getStartTime, 
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Task::getId)
    );

    // --- 15-МИНУТНАЯ СЕТКА ДЛЯ O(1) ПРОВЕРКИ ПЕРЕСЕЧЕНИЙ ---
    private static final int INTERVAL_MINUTES = 15;
    private static final int MINUTES_PER_YEAR = 365 * 24 * 60;
    private static final int INTERVALS_PER_YEAR = MINUTES_PER_YEAR / INTERVAL_MINUTES;
    private final boolean[] scheduleGrid = new boolean[INTERVALS_PER_YEAR];
    private static final LocalDateTime BASE_DATE = LocalDateTime.now()
            .withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

    // Перевод времени в индекс массива
    private Integer timeToGridIndex(LocalDateTime time) {
        if (time == null) return null;
        long minutesFromBase = ChronoUnit.MINUTES.between(BASE_DATE, time);
        if (minutesFromBase < 0 || minutesFromBase >= MINUTES_PER_YEAR) return null;
        return (int) (minutesFromBase / INTERVAL_MINUTES);
    }

    // Проверка свободны ли интервалы в сетке
    private boolean areGridIntervalsFree(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return true;
        }

        Integer startIndex = timeToGridIndex(task.getStartTime());
        Integer endIndex = timeToGridIndex(task.getEndTime());

        if (startIndex == null || endIndex == null) {
            return true; // Задача вне сетки - пропускаем проверку
        }

        for (int i = startIndex; i < endIndex; i++) {
            if (scheduleGrid[i]) {
                return false;
            }
        }
        return true;
    }

    // Занять интервалы в сетке
    protected void occupyGridIntervals(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return;
        }

        Integer startIndex = timeToGridIndex(task.getStartTime());
        Integer endIndex = timeToGridIndex(task.getEndTime());

        if (startIndex != null && endIndex != null) {
            for (int i = startIndex; i < endIndex; i++) {
                scheduleGrid[i] = true;
            }
        }
    }

    // Освободить интервалы в сетке
    private void freeGridIntervals(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return;
        }

        Integer startIndex = timeToGridIndex(task.getStartTime());
        Integer endIndex = timeToGridIndex(task.getEndTime());

        if (startIndex != null && endIndex != null) {
            for (int i = startIndex; i < endIndex; i++) {
                scheduleGrid[i] = false;
            }
        }
    }

    // ГИБРИДНАЯ ПРОВЕРКА: СЕТКА + STREAM API
    protected boolean hasTimeOverlapWithExisting(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getEndTime() == null) {
            return false;
        }

        // 1. Быстрая проверка через сетку O(1)
        if (!areGridIntervalsFree(newTask)) {
            return true;
        }

        // 2. Проверка через Stream API для задач вне сетки
        // УБРАЛИ ЛИШНИЕ ФИЛЬТРЫ - в prioritizedTasks уже только задачи с временем
        return prioritizedTasks.stream()
                .filter(existingTask -> existingTask.getId() != newTask.getId())
                .filter(existingTask -> {
                    // Проверяем только задачи вне сетки
                    Integer startIndex = timeToGridIndex(existingTask.getStartTime());
                    return startIndex == null; // Если null - задача вне сетки
                })
                .anyMatch(existingTask -> isOverlapping(newTask, existingTask));
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
            occupyGridIntervals(task); // ЗАНИМАЕМ ИНТЕРВАЛЫ В СЕТКЕ
        }

        return task.getId();
    }

    @Override
    public void updateTask(Task task) {
        if (task == null || !tasks.containsKey(task.getId())) return;

        Task oldTask = tasks.get(task.getId());
        if (oldTask.getStartTime() != null) {
            prioritizedTasks.remove(oldTask);
            freeGridIntervals(oldTask); // ОСВОБОЖДАЕМ СТАРЫЕ ИНТЕРВАЛЫ
        }

        if (hasTimeOverlapWithExisting(task)) {
            // возвращаем старую задачу
            if (oldTask.getStartTime() != null) {
                prioritizedTasks.add(oldTask);
                occupyGridIntervals(oldTask);
            }
            throw new IllegalArgumentException("Задача пересекается по времени с существующей задачей");
        }

        tasks.put(task.getId(), task);

        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
            occupyGridIntervals(task); // ЗАНИМАЕМ НОВЫЕ ИНТЕРВАЛЫ
        }
    }

    @Override
    public void deleteTaskById(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            historyManager.remove(id);
            if (task.getStartTime() != null) {
                prioritizedTasks.remove(task);
                freeGridIntervals(task); // ОСВОБОЖДАЕМ ИНТЕРВАЛЫ
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
        // УБРАЛИ updateEpicTime() - у нового эпика нет подзадач, время будет сброшено
        return epic.getId();
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic == null || !epics.containsKey(epic.getId())) return;

        Epic savedEpic = epics.get(epic.getId());
        savedEpic.setName(epic.getName());
        savedEpic.setDescription(epic.getDescription());
        // УБРАЛИ updateEpicTime() - при обновлении эпика подзадачи не меняются
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (Integer subtaskId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.remove(subtaskId);
                if (subtask != null && subtask.getStartTime() != null) {
                    prioritizedTasks.remove(subtask);
                    freeGridIntervals(subtask); // ОСВОБОЖДАЕМ ИНТЕРВАЛЫ ПОДЗАДАЧ
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
        updateEpicTime(epic); // ОСТАВИЛИ - при создании подзадачи время эпика меняется

        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
            occupyGridIntervals(subtask); // ЗАНИМАЕМ ИНТЕРВАЛЫ
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
            freeGridIntervals(oldSubtask); // ОСВОБОЖДАЕМ СТАРЫЕ ИНТЕРВАЛЫ
        }

        if (hasTimeOverlapWithExisting(subtask)) {
            if (oldSubtask.getStartTime() != null) {
                prioritizedTasks.add(oldSubtask);
                occupyGridIntervals(oldSubtask);
            }
            throw new IllegalArgumentException("Подзадача пересекается по времени с существующей задачей");
        }

        subtasks.put(subtask.getId(), subtask);
        updateEpicStatus(epic);
        updateEpicTime(epic); // ОСТАВИЛИ - при обновлении подзадачи время эпика может измениться

        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
            occupyGridIntervals(subtask); // ЗАНИМАЕМ НОВЫЕ ИНТЕРВАЛЫ
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
                updateEpicTime(epic); // ОСТАВИЛИ - при удалении подзадачи время эпика меняется
            }
            historyManager.remove(id);
            if (subtask.getStartTime() != null) {
                prioritizedTasks.remove(subtask);
                freeGridIntervals(subtask); // ОСВОБОЖДАЕМ ИНТЕРВАЛЫ
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
                freeGridIntervals(t); // ОСВОБОЖДАЕМ ИНТЕРВАЛЫ
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
                freeGridIntervals(s); // ОСВОБОЖДАЕМ ИНТЕРВАЛЫ
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
                        freeGridIntervals(s); // ОСВОБОЖДАЕМ ИНТЕРВАЛЫ
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
            epic.setStartTime(null);
            epic.setDuration(Duration.ZERO); // ИСПРАВЛЕНИЕ: Duration.ZERO вместо null
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

        epic.setStartTime(earliest);
        epic.setDuration(total);
        epic.setEndTime(latest);
    }
}
