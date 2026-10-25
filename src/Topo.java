/** Topological order by three-state DFS (0 = white, 1 = gray, 2 = black); a gray hit is a directed cycle. */
public final class Topo {
    public static final class Result {
        public final boolean acyclic;
        public final int[] order; // empty when a cycle exists
        Result(boolean acyclic, int[] order) { this.acyclic = acyclic; this.order = order; }
    }

    public static Result run(int n, int[][] edges) {
        return run(n, Csr.build(n, edges, false, false));
    }

    static Result run(int n, Csr g) {
        byte[] state = new byte[n];
        int[] ptr = new int[n];
        int[] stack = new int[n];
        int[] post = new int[n];
        int pc = 0;
        for (int s = 0; s < n; s++) {
            if (state[s] != 0) continue;
            int sp = 0;
            stack[sp++] = s;
            state[s] = 1;
            ptr[s] = g.start[s];
            while (sp > 0) {
                int u = stack[sp - 1];
                if (ptr[u] < g.start[u + 1]) {
                    int v = g.to[ptr[u]++];
                    if (state[v] == 1) return new Result(false, new int[0]); // back edge (also self-loop)
                    if (state[v] == 0) { state[v] = 1; ptr[v] = g.start[v]; stack[sp++] = v; }
                } else {
                    state[u] = 2;
                    post[pc++] = u;
                    sp--;
                }
            }
        }
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = post[n - 1 - i];
        return new Result(true, order);
    }
}
