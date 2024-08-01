package graph;

import java.util.*;

class AdjacencyMatrixGraph<T> implements Graph<T> {
    private final Map<T, Integer> nodeIndex = new HashMap<>();
    private final List<List<Boolean>> adjacencyMatrix = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    public void addEdge(T source, T destination) {
        ensureNodeExists(source);
        ensureNodeExists(destination);
        int sourceIndex = nodeIndex.get(source);
        int destinationIndex = nodeIndex.get(destination);
        adjacencyMatrix.get(sourceIndex).set(destinationIndex, true);
    }

    private void ensureNodeExists(T node) {
        if (!nodeIndex.containsKey(node)) {
            nodeIndex.put(node, currentIndex++);
            for (List<Boolean> row : adjacencyMatrix) {
                row.add(false);
            }
            List<Boolean> newRow = new ArrayList<>(Collections.nCopies(currentIndex, false));
            adjacencyMatrix.add(newRow);
        }
    }

    @Override
    public List<T> getNeighbors(T node) {
        List<T> neighbors = new ArrayList<>();
        if (nodeIndex.containsKey(node)) {
            int nodeIdx = nodeIndex.get(node);
            for (Map.Entry<T, Integer> entry : nodeIndex.entrySet()) {
                if (adjacencyMatrix.get(nodeIdx).get(entry.getValue())) {
                    neighbors.add(entry.getKey());
                }
            }
        }
        return neighbors;
    }

    @Override
    public Set<T> getNodes() {
        return nodeIndex.keySet();
    }
}
