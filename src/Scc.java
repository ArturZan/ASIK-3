import java.util.Arrays;

/** Strongly connected components with Kosaraju: DFS on G for finish order, DFS on transpose in reverse order. */
public final class Scc {
    public static final class Result {
        public final int[] component;      // label per vertex, 0..count-1
        public final int count;
        public final int[][] condensation; // distinct pairs {fromLabel, toLabel}, fromLabel != toLabel, sorted
        Result(int[] component, int count, int[][] condensation) {
            this.component = component; this.count = count; this.condensation = condensation;
        }
    }

    public static Result run(int n, int[][] edges) {
        Csr g = Csr.build(n, edges, false, false);
        Csr gt = Csr.build(n, edges, false, true);

        // pass 1: iterative DFS, record vertices by finishing time
        int[] order = new int[n];
        int oc = 0;
        boolean[] seen = new boolean[n];
        int[] ptr = new int[n];
        int[] stack = new int[n];
        for (int s = 0; s < n; s++) {
            if (seen[s]) continue;
            int sp = 0;
            stack[sp++] = s;
            seen[s] = true;
            ptr[s] = g.start[s];
            while (sp > 0) {
                int u = stack[sp - 1];
                if (ptr[u] < g.start[u + 1]) {
                    int v = g.to[ptr[u]++];
                    if (!seen[v]) { seen[v] = true; ptr[v] = g.start[v]; stack[sp++] = v; }
                } else {
                    order[oc++] = u;
                    sp--;
                }
            }
        }

        // pass 2: DFS on transpose in decreasing finish time; each tree is one SCC
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int count = 0;
        for (int i = n - 1; i >= 0; i--) {
            int s = order[i];
            if (comp[s] != -1) continue;
            int sp = 0;
            stack[sp++] = s;
            comp[s] = count;
            while (sp > 0) {
                int u = stack[--sp];
                for (int k = gt.start[u]; k < gt.start[u + 1]; k++) {
                    int v = gt.to[k];
                    if (comp[v] == -1) { comp[v] = count; stack[sp++] = v; }
                }
            }
            count++;
        }

        // condensation edges, deduplicated
        long[] keys = new long[edges.length];
        int kc = 0;
        for (int[] e : edges) {
            int a = comp[e[0]], b = comp[e[1]];
            if (a != b) keys[kc++] = (long) a * count + b;
        }
        Arrays.sort(keys, 0, kc);
        int[][] pairs = new int[kc][];
        int pc = 0;
        for (int i = 0; i < kc; i++) {
            if (i > 0 && keys[i] == keys[i - 1]) continue;
            pairs[pc++] = new int[] { (int) (keys[i] / count), (int) (keys[i] % count) };
        }
        return new Result(comp, count, Arrays.copyOf(pairs, pc));
    }
}
