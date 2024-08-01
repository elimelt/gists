package graph;
import java.util.*;

class FloydWarshall {
    public static void main(String[] args) {
        int[][] graph = {
                { 0, 5, 999, 10 },
                { 999, 0, 3, 999 },
                { 999, 999, 0, 1 },
                { 999, 999, 999, 0 }
        };

        int[][] dist = floydWarshall(graph);

        for (int i = 0; i < dist.length; i++) {
            System.out.println(Arrays.toString(dist[i]));
        }
    }

    public static int[][] floydWarshall(int[][] graph) {
        int n = graph.length;
        int[][] dist = new int[n][n];

        for (int i = 0; i < n; i++) {
            dist[i] = Arrays.copyOf(graph[i], n);
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                }
            }
        }

        return dist;
    }

}
