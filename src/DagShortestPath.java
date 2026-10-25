import java.util.Arrays;

/** Single-source shortest paths on a DAG (negative weights allowed) + route reconstruction. */
public final class DagShortestPath {
    public static final class Result {
        public final boolean acyclic;
        public final long[] distance;     // meaningful only where reachable[v]
        public final boolean[] reachable; // false -> output null
        public final int[] parentEdge;    // original edge id, -1 for source/unreachable
        Result(boolean acyclic, long[] distance, boolean[] reachable, int[] parentEdge) {
            this.acyclic = acyclic; this.distance = distance; this.reachable = reachable; this.parentEdge = parentEdge;
        }
    }

    public static Result run(int n, int[][] edges, int source) {
        Csr g = Csr.build(n, edges, false, false);
        Topo.Result t = Topo.run(n, g); // whole graph is checked, even parts unreachable from source
        if (!t.acyclic) return new Result(false, new long[0], new boolean[0], new int[0]);
        long[] dist = new long[n];
        boolean[] reach = new boolean[n];
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        reach[source] = true;
        for (int u : t.order) {
            if (!reach[u]) continue;
            for (int k = g.start[u]; k < g.start[u + 1]; k++) {
                int v = g.to[k];
                int e = g.id[k];
                long nd = dist[u] + edges[e][2];
                if (!reach[v] || nd < dist[v]) { // strict: first edge wins ties
                    reach[v] = true;
                    dist[v] = nd;
                    parent[v] = e;
                }
            }
        }
        return new Result(true, dist, reach, parent);
    }

    /** Edge ids on the route source -> target, in order. Empty if target == source, null if unreachable. */
    public static int[] routeEdges(int source, int target, int[] parentEdge, int[][] edges) {
        if (parentEdge.length == 0) return null;
        int[] rev = new int[parentEdge.length];
        int len = 0;
        int cur = target;
        while (cur != source) {
            int e = parentEdge[cur];
            if (e < 0 || len >= rev.length) return null;
            rev[len++] = e;
            cur = edges[e][0];
        }
        int[] out = new int[len];
        for (int i = 0; i < len; i++) out[i] = rev[len - 1 - i];
        return out;
    }

    /** Vertices on the route source -> target (inclusive), or null if unreachable. */
    public static int[] routeVertices(int source, int target, int[] parentEdge, int[][] edges) {
        int[] es = routeEdges(source, target, parentEdge, edges);
        if (es == null) return null;
        int[] vs = new int[es.length + 1];
        vs[0] = source;
        for (int i = 0; i < es.length; i++) vs[i + 1] = edges[es[i]][1];
        return vs;
    }
}
