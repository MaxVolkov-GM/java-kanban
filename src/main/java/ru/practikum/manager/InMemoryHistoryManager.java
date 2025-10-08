package ru.practikum.manager;

import ru.practikum.model.Task;
import ru.practikum.model.Epic;
import ru.practikum.model.Subtask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {

    private static class Node {
        Task task;
        Node prev;
        Node next;

        Node(Task task) {
            this.task = task;
        }
    }

    private final Map<Integer, Node> nodeMap = new HashMap<>();
    private Node first;
    private Node last;

    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }

        int id = task.getId();
        remove(id);

        linkLast(task);
        nodeMap.put(id, last);
    }

    @Override
    public void remove(int id) {
        Node node = nodeMap.get(id);
        if (node != null) {
            removeNode(node);
            nodeMap.remove(id);
        }
    }

    @Override
    public List<Task> getHistory() {
        return getTasks();
    }

    private void linkLast(Task task) {
        Task taskCopy = copyTask(task);
        Node newNode = new Node(taskCopy);

        if (last == null) {
            first = newNode;
            last = newNode;
        } else {
            last.next = newNode;
            newNode.prev = last;
            last = newNode;
        }
    }

    private List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();
        Node current = first;

        while (current != null) {
            tasks.add(current.task);
            current = current.next;
        }

        return tasks;
    }

    private void removeNode(Node node) {
        if (node == null) {
            return;
        }

        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            first = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            last = node.prev;
        }

        node.prev = null;
        node.next = null;
    }

    private Task copyTask(Task original) {
        if (original instanceof Epic) {
            return new Epic((Epic) original);
        } else if (original instanceof Subtask) {
            return new Subtask((Subtask) original);
        } else {
            return new Task(original);
        }
    }
}