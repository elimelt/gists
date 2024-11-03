package graph;

import java.util.*;

public class GraphStressTest {
    private static final int NUM_NODES = 10000;
    private static final int NUM_EDGES = 50000;

    public static void main(String[] args) {
        Random random = new Random();
        Graph<Integer> adjacencyListGraph = new AdjacencyListGraph<>();
        Graph<Integer> adjacencyMatrixGraph = new AdjacencyMatrixGraph<>();

        // Populate graphs with random edges
        for (int i = 0; i < NUM_EDGES; i++) {
            int source = random.nextInt(NUM_NODES);
            int destination = random.nextInt(NUM_NODES);
            adjacencyListGraph.addEdge(source, destination);
            adjacencyMatrixGraph.addEdge(source, destination);
        }

        // Stress test BFS
        stressTestBFS(adjacencyListGraph, "Adjacency List Graph");
        stressTestBFS(adjacencyMatrixGraph, "Adjacency Matrix Graph");

        // Stress test Dijkstra's Algorithm
        Map<Integer, Map<Integer, Integer>> edgeWeights = generateEdgeWeights(adjacencyListGraph);
        stressTestDijkstra(adjacencyListGraph, edgeWeights, "Adjacency List Graph");
        stressTestDijkstra(adjacencyMatrixGraph, edgeWeights, "Adjacency Matrix Graph");
    }

    private static void stressTestBFS(Graph<Integer> graph, String graphType) {
        BreadthFirstSearch<Integer> bfs = new BreadthFirstSearch<>();
        long startTime = System.nanoTime();
        bfs.getLevels(graph, 0);
        long endTime = System.nanoTime();
        System.out.printf("BFS on %s took %.2f ms%n", graphType, (endTime - startTime) / 1e6);
    }

    private static void stressTestDijkstra(Graph<Integer> graph, Map<Integer, Map<Integer, Integer>> edgeWeights,
            String graphType) {
        DijkstrasAlgorithm<Integer> dijkstra = new DijkstrasAlgorithm<>();
        long startTime = System.nanoTime();
        dijkstra.findShortestPaths(graph, 0, edgeWeights);
        long endTime = System.nanoTime();
        System.out.printf("Dijkstra's Algorithm on %s took %.2f ms%n", graphType, (endTime - startTime) / 1e6);
    }

    private static Map<Integer, Map<Integer, Integer>> generateEdgeWeights(Graph<Integer> graph) {
        Random random = new Random();
        Map<Integer, Map<Integer, Integer>> edgeWeights = new HashMap<>();
        for (Integer node : graph.getNodes()) {
            edgeWeights.put(node, new HashMap<>());
            for (Integer neighbor : graph.getNeighbors(node)) {
                edgeWeights.get(node).put(neighbor, random.nextInt(10) + 1);
            }
        }
        return edgeWeights;
    }
}
