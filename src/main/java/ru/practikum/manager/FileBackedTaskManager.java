package ru.practikum.manager;

import ru.practikum.model.Epic;
import ru.practikum.model.Status;
import ru.practikum.model.Subtask;
import ru.practikum.model.Task;
import ru.practikum.model.TaskType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    protected void save() {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("id,type,name,status,description,epic\n");

            for (Task task : getAllTasks()) {
                writer.write(taskToString(task) + "\n");
            }
            for (Epic epic : getAllEpics()) {
                writer.write(taskToString(epic) + "\n");
            }
            for (Subtask subtask : getAllSubtasks()) {
                writer.write(taskToString(subtask) + "\n");
            }

            // Сохраняем историю просмотров
            writer.write("\n");
            writer.write(historyToString(historyManager));

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка сохранения в файл", e);
        }
    }

    private String taskToString(Task task) {
        String type = getType(task).toString();
        String epicId = (task instanceof Subtask) ? String.valueOf(((Subtask) task).getEpicId()) : "";

        return String.format("%d,%s,%s,%s,%s,%s",
            task.getId(),
            type,
            escapeString(task.getName()),
            task.getStatus(),
            escapeString(task.getDescription()),
            epicId);
    }

    private String escapeString(String text) {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private TaskType getType(Task task) {
        if (task instanceof Epic) return TaskType.EPIC;
        if (task instanceof Subtask) return TaskType.SUBTASK;
        return TaskType.TASK;
    }

    private static Task fromString(String value) {
        String[] parts = parseCsvLine(value);

        int id = Integer.parseInt(parts[0]);
        TaskType type = TaskType.valueOf(parts[1]);
        String name = unescapeString(parts[2]);
        Status status = Status.valueOf(parts[3]);
        String description = unescapeString(parts[4]);
        String epicIdStr = parts.length > 5 ? parts[5] : "";

        switch (type) {
            case TASK:
                Task task = new Task(name, description, status);
                task.setId(id);
                return task;
            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                // Статус эпика вычисляется автоматически на основе подзадач
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(epicIdStr);
                Subtask subtask = new Subtask(name, description, status, epicId);
                subtask.setId(id);
                return subtask;
            default:
                throw new IllegalArgumentException("Unknown task type: " + type);
        }
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());
        return result.toArray(new String[0]);
    }

    private static String unescapeString(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1).replace("\"\"", "\"");
        }
        return text;
    }

    // Метод для сериализации истории
    public static String historyToString(HistoryManager manager) {
        List<Task> history = manager.getHistory();
        List<String> historyIds = new ArrayList<>();
        for (Task task : history) {
            historyIds.add(String.valueOf(task.getId()));
        }
        return String.join(",", historyIds);
    }

    // Метод для десериализации истории
    public static List<Integer> historyFromString(String value) {
        List<Integer> historyIds = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return historyIds;
        }
        String[] ids = value.split(",");
        for (String id : ids) {
            try {
                historyIds.add(Integer.parseInt(id.trim()));
            } catch (NumberFormatException e) {
                // Игнорируем некорректные ID
            }
        }
        return historyIds;
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        try {
            if (!file.exists()) {
                return manager;
            }

            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");

            boolean historySection = false;
            List<Integer> historyIds = new ArrayList<>();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                if (line.isEmpty()) {
                    historySection = true;
                    continue;
                }

                if (i == 0 && line.startsWith("id,type")) {
                    continue; // пропускаем заголовок
                }

                if (!historySection) {
                    Task task = fromString(line);
                    if (task instanceof Epic) {
                        manager.epics.put(task.getId(), (Epic) task);
                    } else if (task instanceof Subtask) {
                        manager.subtasks.put(task.getId(), (Subtask) task);
                    } else {
                        manager.tasks.put(task.getId(), task);
                    }

                    if (task.getId() > manager.sequence) {
                        manager.sequence = task.getId();
                    }
                } else {
                    // Загружаем историю
                    historyIds = historyFromString(line);
                    break; // история всегда последняя
                }
            }

            // Восстанавливаем связи для подзадач
            for (Subtask subtask : manager.subtasks.values()) {
                Epic epic = manager.epics.get(subtask.getEpicId());
                if (epic != null) {
                    epic.addSubtaskId(subtask.getId());
                }
            }

            // Восстанавливаем историю просмотров
            for (Integer taskId : historyIds) {
                Task task = manager.findTaskById(taskId);
                if (task != null) {
                    manager.historyManager.add(task);
                }
            }

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка загрузки из файла", e);
        }

        return manager;
    }

    // Вспомогательный метод для поиска задачи по ID
    private Task findTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) return task;

        task = epics.get(id);
        if (task != null) return task;

        return subtasks.get(id);
    }

    @Override
    public int createTask(Task task) {
        int result = super.createTask(task);
        save();
        return result;
    }

    @Override
    public int createEpic(Epic epic) {
        int result = super.createEpic(epic);
        save();
        return result;
    }

    @Override
    public int createSubtask(Subtask subtask) {
        int result = super.createSubtask(subtask);
        save();
        return result;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    @Override
    public void deleteTasks() {
        super.deleteTasks();
        save();
    }

    @Override
    public void deleteEpics() {
        super.deleteEpics();
        save();
    }

    @Override
    public void deleteSubtasks() {
        super.deleteSubtasks();
        save();
    }
}
