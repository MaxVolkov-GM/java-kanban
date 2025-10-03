package ru.practikum;

import ru.practikum.manager.Managers;
import ru.practikum.manager.TaskManager;
import ru.practikum.model.Epic;
import ru.practikum.model.Status;
import ru.practikum.model.Subtask;
import ru.practikum.model.Task;


public class Main {
    public static void main(String[] args) {
        TaskManager manager = Managers.getDefault();
        System.out.println("Поехали! Начинаем работу с задачами.\n");

        // Создаем обычные задачи
        int taskId1 = manager.createTask(new Task("Купить продукты", "Молоко, хлеб, яйца", Status.NEW));
        int taskId2 = manager.createTask(new Task("Записаться к врачу", "Терапевт на следующей неделе", Status.NEW));
        System.out.println("Созданы две обычные задачи");

        // Создаем эпики с подзадачами
        int epicId1 = manager.createEpic(new Epic("Ремонт в квартире", "Полный ремонт"));
        int subtask1 = manager.createSubtask(new Subtask("Купить материалы", "Краска, обои, инструменты", Status.NEW, epicId1));
        int subtask2 = manager.createSubtask(new Subtask("Нанять рабочих", "Найти проверенную бригаду", Status.NEW, epicId1));

        int epicId2 = manager.createEpic(new Epic("Планирование отпуска", "Отпуск летом"));
        int subtask3 = manager.createSubtask(new Subtask("Выбрать направление", "Испания или Италия", Status.NEW, epicId2));
        System.out.println("Созданы два эпика с подзадачами");

        // Просматриваем задачи
        System.out.println("\nПросматриваем задачи...");
        manager.getTaskById(taskId1);
        manager.getEpicById(epicId1);
        manager.getSubtaskById(subtask1);
        printAllTasks(manager);

        // Изменяем статусы задач
        System.out.println("Обновляем статусы задач...");
        Task task1 = manager.getTaskById(taskId1);
        task1.setStatus(Status.IN_PROGRESS);
        manager.updateTask(task1);

        Subtask subtask1Obj = manager.getSubtaskById(subtask1);
        subtask1Obj.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(subtask1Obj);

        Subtask subtask2Obj = manager.getSubtaskById(subtask2);
        subtask2Obj.setStatus(Status.DONE);
        manager.updateSubtask(subtask2Obj);

        Subtask subtask3Obj = manager.getSubtaskById(subtask3);
        subtask3Obj.setStatus(Status.DONE);
        manager.updateSubtask(subtask3Obj);

        // Снова просматриваем задачи
        System.out.println("\nПроверяем обновленные статусы...");
        manager.getTaskById(taskId1);
        manager.getEpicById(epicId1);
        manager.getSubtaskById(subtask1);
        manager.getEpicById(epicId2);
        printAllTasks(manager);

        // Демонстрируем ограничение истории
        System.out.println("Создаем дополнительные задачи для демонстрации истории...");
        for (int i = 1; i <= 12; i++) {
            int id = manager.createTask(new Task("Доп. задача " + i, "Описание " + i, Status.NEW));
            manager.getTaskById(id);
        }
        System.out.println("\nИстория после добавления 12 задач (должна содержать только 10 последних):");
        printAllTasks(manager);
    }

    private static void printAllTasks(TaskManager manager) {
        System.out.println("\n===== ТЕКУЩЕЕ СОСТОЯНИЕ ЗАДАЧ =====");

        System.out.println("\nОбычные задачи:");
        for (Task task : manager.getAllTasks()) {
            System.out.println("  " + task);
        }

        System.out.println("\nЭпики:");
        for (Epic epic : manager.getAllEpics()) {
            System.out.println("  " + epic);
            System.out.println("    Подзадачи:");
            for (Subtask subtask : manager.getSubtasksByEpicId(epic.getId())) {
                System.out.println("    - " + subtask);
            }
        }

        System.out.println("\nИстория просмотров (последние " + manager.getHistory().size() + " из 10):");
        for (Task task : manager.getHistory()) {
            System.out.println("  " + task);
        }
        System.out.println("=".repeat(50) + "\n");
    }
}