import java.util.Arrays;

/**
 * Minimum spanning forest by (lazy) Prim. The heap holds (weight, edge id) of edges leaving the visited
 * set; Prim is restarted from every still-unvisited vertex, so each component gets its own tree.
 */
public final class Prim {
    private static final int ID_BITS = 20;

    public static Kruskal.Result run(int n, int[][] edges) {
        Csr g = Csr.build(n, edges, true, false);
        boolean[] visited = new boolean[n];
        LongHeap heap = new LongHeap();
        int[] picked = new int[Math.max(0, n - 1)];
        int cnt = 0;
        long cost = 0;
        int components = 0;
        for (int s = 0; s < n; s++) {
            if (visited[s]) continue;
            components++;
            visited[s] = true;
            pushEdges(g, edges, s, visited, heap);
            while (!heap.isEmpty()) {
                int id = (int) (heap.pop() & ((1L << ID_BITS) - 1));
                int u = edges[id][0], v = edges[id][1];
                if (visited[u] && visited[v]) continue; // stale entry or loop
                int t = visited[u] ? v : u;
                visited[t] = true;
                picked[cnt++] = id;
                cost += edges[id][2];
                pushEdges(g, edges, t, visited, heap);
            }
        }
        return new Kruskal.Result(cost, Arrays.copyOf(picked, cnt), components);
    }

    private static void pushEdges(Csr g, int[][] edges, int v, boolean[] visited, LongHeap heap) {
        for (int k = g.start[v]; k < g.start[v + 1]; k++) {
            if (visited[g.to[k]]) continue;
            int id = g.id[k];
            heap.push(((long) edges[id][2] + 1_000_000_000L << ID_BITS) | id);
        }
    }
}
