package ru.practikum.manager;

import ru.practikum.model.*;
import ru.practikum.model.TaskType;
import ru.practikum.exception.*;

import java.io.*;
import java.nio.file.Files;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private static final String CSV_HEADER = "id,type,name,status,description,epic";
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    // Метод автосохранения
    protected void save() {
        try (PrintWriter writer = new PrintWriter(file)) {
            // Записываем заголовок CSV
            writer.println(CSV_HEADER);

            // Сохраняем задачи
            for (Task task : getAllTasks()) {
                writer.println(toString(task));
            }

            // Сохраняем эпики
            for (Epic epic : getAllEpics()) {
                writer.println(toString(epic));
            }

            // Сохраняем подзадачи
            for (Subtask subtask : getAllSubtasks()) {
                writer.println(toString(subtask));
            }

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка сохранения в файл", e);
        }
    }

    // Преобразование задачи в строку CSV
    private String toString(Task task) {
        TaskType type = task.getType();
        switch (type) {
            case EPIC:
                return String.format("%d,EPIC,%s,%s,%s,",
                        task.getId(), task.getName(), task.getStatus(), task.getDescription());
            case SUBTASK:
                Subtask subtask = (Subtask) task;
                return String.format("%d,SUBTASK,%s,%s,%s,%d",
                        subtask.getId(), subtask.getName(), subtask.getStatus(),
                        subtask.getDescription(), subtask.getEpicId());
            case TASK:
            default:
                return String.format("%d,TASK,%s,%s,%s,",
                        task.getId(), task.getName(), task.getStatus(), task.getDescription());
        }
    }

    // Восстановление задачи из строки CSV
    private Task fromString(String value) {
        String[] fields = value.split(",");
        int id = Integer.parseInt(fields[0]);
        TaskType type = TaskType.valueOf(fields[1]);
        String name = fields[2];
        Status status = Status.valueOf(fields[3]);
        String description = fields[4];

        switch (type) {
            case TASK:
                Task task = new Task(name, description, status);
                task.setId(id);
                return task;
            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(fields[5]);
                Subtask subtask = new Subtask(name, description, status, epicId);
                subtask.setId(id);
                return subtask;
            default:
                throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
        }
    }

    // Переопределение методов с автосохранением
    @Override
    public int createTask(Task task) {
        int result = super.createTask(task);
        save();
        return result;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public int createEpic(Epic epic) {
        int result = super.createEpic(epic);
        save();
        return result;
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public int createSubtask(Subtask subtask) {
        int result = super.createSubtask(subtask);
        save();
        return result;
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    // Статический метод загрузки из файла
    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        try {
            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");

            // Пропускаем заголовок и пустые строки
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                Task task = manager.fromString(line);
                if (task instanceof Epic) {
                    manager.epics.put(task.getId(), (Epic) task);
                } else if (task instanceof Subtask) {
                    manager.subtasks.put(task.getId(), (Subtask) task);
                } else {
                    manager.tasks.put(task.getId(), task);
                }

                // Обновляем nextId
                if (task.getId() >= manager.nextId) {
                    manager.nextId = task.getId() + 1;
                }
            }

            // Восстанавливаем связи эпиков и подзадач
            for (Subtask subtask : manager.subtasks.values()) {
                Epic epic = manager.epics.get(subtask.getEpicId());
                if (epic != null) {
                    epic.addSubtaskId(subtask.getId());
                }
            }

        } catch (IOException e) {
            throw new ManagerLoadException("Ошибка загрузки из файла", e);
        }

        return manager;
    }
}
