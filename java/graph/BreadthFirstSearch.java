package graph;

import java.util.*;

public class BreadthFirstSearch<T> {
    public List<T> searchFlat(Graph<T> graph, T startNode) {
        List<T> result = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Queue<T> queue = new LinkedList<>();
        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            T current = queue.poll();
            result.add(current);
            for (T neighbor : graph.getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    public List<List<T>> getLevels(Graph<T> graph, T startNode) {
        List<List<T>> levels = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Queue<T> queue = new LinkedList<>();
        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            List<T> currentLevel = new ArrayList<>();
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                T current = queue.poll();
                currentLevel.add(current);
                for (T neighbor : graph.getNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            levels.add(currentLevel);
        }

        return levels;
    }
}
