/** Compressed adjacency lists (CSR). Loops are kept; callers decide what to do with them. */
final class Csr {
    final int[] start; // size n+1
    final int[] to;    // neighbour vertex
    final int[] id;    // original edge id

    private Csr(int[] start, int[] to, int[] id) {
        this.start = start;
        this.to = to;
        this.id = id;
    }

    /** directed: u->v only (or v->u when reverse). undirected: both directions, loops once. */
    static Csr build(int n, int[][] edges, boolean undirected, boolean reverse) {
        int[] start = new int[n + 1];
        for (int[] e : edges) {
            int a = reverse ? e[1] : e[0];
            int b = reverse ? e[0] : e[1];
            start[a + 1]++;
            if (undirected && a != b) start[b + 1]++;
        }
        for (int i = 0; i < n; i++) start[i + 1] += start[i];
        int[] pos = new int[n];
        System.arraycopy(start, 0, pos, 0, n);
        int total = start[n];
        int[] to = new int[total];
        int[] id = new int[total];
        for (int i = 0; i < edges.length; i++) {
            int a = reverse ? edges[i][1] : edges[i][0];
            int b = reverse ? edges[i][0] : edges[i][1];
            to[pos[a]] = b; id[pos[a]++] = i;
            if (undirected && a != b) { to[pos[b]] = a; id[pos[b]++] = i; }
        }
        return new Csr(start, to, id);
    }
}
