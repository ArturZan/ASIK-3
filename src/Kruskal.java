import java.util.Arrays;

/** Minimum spanning forest by Kruskal: sort edges, add an edge if it joins two different DSU sets. */
public final class Kruskal {
    public static final class Result {
        public final long cost;
        public final int[] edgeIds;
        public final int components;
        Result(long cost, int[] edgeIds, int components) {
            this.cost = cost; this.edgeIds = edgeIds; this.components = components;
        }
    }

    private static final int ID_BITS = 20; // up to ~1M edges

    public static Result run(int n, int[][] edges) {
        int m = edges.length;
        long[] keys = new long[m];
        for (int i = 0; i < m; i++) {
            keys[i] = ((long) edges[i][2] + 1_000_000_000L << ID_BITS) | i; // weight shifted to be >= 0; ties by id
        }
        Arrays.sort(keys);
        Dsu dsu = new Dsu(n);
        int[] picked = new int[Math.max(0, n - 1)];
        int cnt = 0;
        long cost = 0;
        for (int i = 0; i < m && cnt < n - 1; i++) {
            int id = (int) (keys[i] & ((1L << ID_BITS) - 1));
            if (dsu.union(edges[id][0], edges[id][1])) {
                picked[cnt++] = id;
                cost += edges[id][2];
            }
        }
        return new Result(cost, Arrays.copyOf(picked, cnt), n - cnt);
    }
}
