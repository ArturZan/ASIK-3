/** Disjoint set union: path compression (iterative) + union by size. */
public final class Dsu {
    private final int[] parent;
    private final int[] size;
    private int components;
    private int largest;

    public Dsu(int n) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
        components = n;
        largest = n > 0 ? 1 : 0;
    }

    public int find(int x) {
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != root) { int next = parent[x]; parent[x] = root; x = next; }
        return root;
    }

    public boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (size[ra] < size[rb]) { int t = ra; ra = rb; rb = t; }
        parent[rb] = ra;
        size[ra] += size[rb];
        components--;
        if (size[ra] > largest) largest = size[ra];
        return true;
    }

    public int components() { return components; }
    public int largest() { return largest; }

    /** states[i] = {components, largest} after the i-th union operation. */
    public static int[][] run(int n, int[][] operations) {
        Dsu d = new Dsu(n);
        int[][] states = new int[operations.length][];
        for (int i = 0; i < operations.length; i++) {
            d.union(operations[i][0], operations[i][1]);
            states[i] = new int[] { d.components(), d.largest() };
        }
        return states;
    }
}
