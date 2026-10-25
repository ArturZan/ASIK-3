import java.util.*;

/** Self-contained tests (no framework). Run: java -cp out AlgoTests  (exit code 1 on any failure). */
public final class AlgoTests {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("PASS  " + name); }
        else { failed++; System.out.println("FAIL  " + name); }
    }

    // ---------- independent reference helpers (deliberately simple and different from src/) ----------

    static int[] naiveLabels(int n, int[][] edges, int[] use) {
        int[] lab = new int[n];
        for (int i = 0; i < n; i++) lab[i] = i;
        for (int id : use) {
            int a = lab[edges[id][0]], b = lab[edges[id][1]];
            if (a == b) continue;
            for (int i = 0; i < n; i++) if (lab[i] == b) lab[i] = a;
        }
        return lab;
    }

    static int countDistinct(int[] lab) {
        return (int) Arrays.stream(lab).distinct().count();
    }

    static int[] allIds(int m) { return java.util.stream.IntStream.range(0, m).toArray(); }

    /** Valid minimum-spanning-forest witness: distinct ids, acyclic, spanning every component, cost matches. */
    static boolean validForest(int n, int[][] edges, Kruskal.Result r) {
        Set<Integer> seen = new HashSet<>();
        int[] lab = new int[n];
        for (int i = 0; i < n; i++) lab[i] = i;
        long cost = 0;
        for (int id : r.edgeIds) {
            if (id < 0 || id >= edges.length || !seen.add(id)) return false;
            int a = lab[edges[id][0]], b = lab[edges[id][1]];
            if (a == b) return false; // would close a cycle (or loop)
            for (int i = 0; i < n; i++) if (lab[i] == b) lab[i] = a;
            cost += edges[id][2];
        }
        int fullComps = countDistinct(naiveLabels(n, edges, allIds(edges.length)));
        return cost == r.cost && r.components == fullComps && r.edgeIds.length == n - fullComps;
    }

    static long bruteMstCost(int n, int[][] edges) {
        int m = edges.length;
        int fullComps = countDistinct(naiveLabels(n, edges, allIds(m)));
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << m); mask++) {
            if (Integer.bitCount(mask) != n - fullComps) continue;
            int[] use = new int[Integer.bitCount(mask)];
            int c = 0;
            long cost = 0;
            for (int i = 0; i < m; i++) if ((mask >> i & 1) != 0) { use[c++] = i; cost += edges[i][2]; }
            int[] lab = naiveLabels(n, edges, use);
            if (countDistinct(lab) != fullComps) continue; // not spanning
            best = Math.min(best, cost);
        }
        return best == Long.MAX_VALUE ? 0 : best;
    }

    static boolean[][] closure(int n, int[][] edges) { // reach[i][j]: path of length >= 1
        boolean[][] r = new boolean[n][n];
        for (int[] e : edges) r[e[0]][e[1]] = true;
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) if (r[i][k]) for (int j = 0; j < n; j++) if (r[k][j]) r[i][j] = true;
        return r;
    }

    static boolean validScc(int n, int[][] edges, Scc.Result r) {
        boolean[][] c = closure(n, edges);
        for (int i = 0; i < n; i++) {
            if (r.component[i] < 0 || r.component[i] >= n) return false;
            for (int j = 0; j < n; j++) {
                boolean same = i == j || (c[i][j] && c[j][i]);
                if (same != (r.component[i] == r.component[j])) return false;
            }
        }
        Set<Long> expect = new TreeSet<>();
        for (int[] e : edges) if (r.component[e[0]] != r.component[e[1]]) expect.add((long) r.component[e[0]] * n + r.component[e[1]]);
        Set<Long> got = new TreeSet<>();
        for (int[] p : r.condensation) {
            if (p[0] == p[1] || !got.add((long) p[0] * n + p[1])) return false; // loop or duplicate pair
        }
        return got.equals(expect);
    }

    static boolean validTopo(int n, int[][] edges, Topo.Result r) {
        boolean[][] c = closure(n, edges);
        boolean cyc = false;
        for (int i = 0; i < n; i++) cyc |= c[i][i];
        if (r.acyclic == cyc) return false;
        if (cyc) return r.order.length == 0;
        if (r.order.length != n) return false;
        int[] pos = new int[n];
        Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) {
            if (r.order[i] < 0 || r.order[i] >= n || pos[r.order[i]] != -1) return false;
            pos[r.order[i]] = i;
        }
        for (int[] e : edges) if (pos[e[0]] >= pos[e[1]]) return false;
        return true;
    }

    static boolean validDag(int n, int[][] edges, int source, DagShortestPath.Result r) {
        boolean[][] c = closure(n, edges);
        boolean cyc = false;
        for (int i = 0; i < n; i++) cyc |= c[i][i];
        if (r.acyclic == cyc) return false;
        if (cyc) return r.distance.length == 0 && r.reachable.length == 0 && r.parentEdge.length == 0;
        // Bellman-Ford oracle
        long INF = Long.MAX_VALUE / 4;
        long[] d = new long[n];
        Arrays.fill(d, INF);
        d[source] = 0;
        for (int it = 0; it < n; it++) for (int[] e : edges) if (d[e[0]] < INF && d[e[0]] + e[2] < d[e[1]]) d[e[1]] = d[e[0]] + e[2];
        for (int v = 0; v < n; v++) {
            boolean reach = d[v] < INF;
            if (reach != r.reachable[v]) return false;
            if (!reach) { if (r.parentEdge[v] != -1) return false; continue; }
            if (r.distance[v] != d[v]) return false;
            if (v == source) { if (r.parentEdge[v] != -1 || r.distance[v] != 0) return false; continue; }
            int p = r.parentEdge[v];
            if (p < 0 || p >= edges.length || edges[p][1] != v) return false;
            if (!(r.reachable[edges[p][0]] && r.distance[edges[p][0]] + edges[p][2] == d[v])) return false;
        }
        return true;
    }

    // ---------- generators ----------

    static int[][] randEdges(Random rnd, int n, int m, int wlo, int whi, boolean weighted) {
        int[][] e = new int[m][];
        for (int i = 0; i < m; i++) {
            int u = rnd.nextInt(n), v = rnd.nextInt(n);
            e[i] = weighted ? new int[] { u, v, wlo + rnd.nextInt(whi - wlo + 1) } : new int[] { u, v };
        }
        return e;
    }

    static int[][] randDag(Random rnd, int n, int m, int wlo, int whi) {
        int[] perm = java.util.stream.IntStream.range(0, n).toArray();
        for (int i = n - 1; i > 0; i--) { int j = rnd.nextInt(i + 1); int t = perm[i]; perm[i] = perm[j]; perm[j] = t; }
        int[][] e = new int[m][];
        for (int i = 0; i < m; i++) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            while (a == b) b = rnd.nextInt(n);
            if (a > b) { int t = a; a = b; b = t; }
            e[i] = new int[] { perm[a], perm[b], wlo + rnd.nextInt(whi - wlo + 1) };
        }
        return e;
    }

    static String json(String task, int n, String key, int[][] rows, String extra) {
        StringBuilder sb = new StringBuilder("{\"task\":\"" + task + "\",\"n\":" + n + extra + ",\"" + key + "\":[");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Arrays.toString(rows[i]).replace(" ", ""));
        }
        return sb.append("]}").toString();
    }

    // ---------- tests ----------

    static void dsuTests() {
        check("dsu n=0, no operations", Dsu.run(0, new int[0][]).length == 0);
        check("dsu n=1 self union", Arrays.deepEquals(Dsu.run(1, new int[][] { { 0, 0 } }), new int[][] { { 1, 1 } }));
        int[][] ops = { { 0, 1 }, { 1, 0 }, { 2, 3 }, { 1, 3 }, { 4, 4 } };
        check("dsu hand case (repeat pair, loop, merge of two sets)",
                Arrays.deepEquals(Dsu.run(5, ops), new int[][] { { 4, 2 }, { 4, 2 }, { 3, 2 }, { 2, 4 }, { 2, 4 } }));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 300 && ok; t++) {
            int n = 1 + rnd.nextInt(12);
            int[][] o = randEdges(rnd, n, rnd.nextInt(20), 0, 0, false);
            int[][] got = Dsu.run(n, o);
            int[] lab = new int[n];
            for (int i = 0; i < n; i++) lab[i] = i;
            for (int i = 0; i < o.length; i++) {
                int a = lab[o[i][0]], b = lab[o[i][1]];
                if (a != b) for (int k = 0; k < n; k++) if (lab[k] == b) lab[k] = a;
                int[] cnt = new int[n];
                int big = 0;
                for (int k = 0; k < n; k++) big = Math.max(big, ++cnt[lab[k]]);
                if (got[i][0] != countDistinct(lab) || got[i][1] != big) ok = false;
            }
        }
        check("dsu 300 random cases vs naive relabeling", ok);
        // deep: chain unions in worst-case order, 250000 operations
        int n = 100000;
        int[][] big = new int[250000][];
        for (int i = 0; i < big.length; i++) big[i] = i < n - 1 ? new int[] { i, i + 1 } : new int[] { i % n, (i * 7) % n };
        int[][] st = Dsu.run(n, big);
        check("dsu full limit n=100000, 250000 ops: ends in one component of size n", st[st.length - 1][0] == 1 && st[st.length - 1][1] == n);
    }

    static void mstTests() {
        int[][] hand = { { 0, 1, 4 }, { 1, 2, -3 }, { 0, 2, 5 }, { 3, 3, 1 }, { 0, 1, 2 } };
        for (String name : new String[] { "kruskal", "prim" }) {
            Kruskal.Result r = name.equals("kruskal") ? Kruskal.run(5, hand) : Prim.run(5, hand);
            check(name + " hand case (loop, parallel, negative, isolate): cost -1, 3 components",
                    r.cost == -1 && r.components == 3 && validForest(5, hand, r));
            Kruskal.Result e = name.equals("kruskal") ? Kruskal.run(0, new int[0][]) : Prim.run(0, new int[0][]);
            check(name + " empty graph", e.cost == 0 && e.components == 0 && e.edgeIds.length == 0);
            Kruskal.Result iso = name.equals("kruskal") ? Kruskal.run(4, new int[0][]) : Prim.run(4, new int[0][]);
            check(name + " only isolates", iso.cost == 0 && iso.components == 4 && iso.edgeIds.length == 0);
        }
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 400 && ok; t++) {
            int n = 1 + rnd.nextInt(6), m = rnd.nextInt(10);
            int[][] e = randEdges(rnd, n, m, -5, 5, true);
            long best = bruteMstCost(n, e);
            Kruskal.Result k = Kruskal.run(n, e), p = Prim.run(n, e);
            ok = validForest(n, e, k) && validForest(n, e, p) && k.cost == best && p.cost == best;
        }
        check("kruskal+prim 400 random tiny graphs vs brute-force optimum (validity, not identity)", ok);

        // invalid witnesses must be rejected by the validator itself
        int[][] tri = { { 0, 1, 1 }, { 1, 2, 1 }, { 0, 2, 1 } };
        check("validator accepts alternative optimal forest {0,2}", validForest(3, tri, new Kruskal.Result(2, new int[] { 0, 2 }, 1)));
        check("validator rejects cycle witness", !validForest(3, tri, new Kruskal.Result(3, new int[] { 0, 1, 2 }, 1)));
        check("validator rejects wrong cost", !validForest(3, tri, new Kruskal.Result(5, new int[] { 0, 1 }, 1)));

        // 64-bit totals: path of 100000 vertices, every weight 1e9 -> 99999e9 > 2^31
        int n = 100000;
        int[][] path = new int[n - 1][];
        for (int i = 0; i < n - 1; i++) path[i] = new int[] { i, i + 1, 1_000_000_000 };
        check("kruskal 64-bit total", Kruskal.run(n, path).cost == 99999L * 1_000_000_000L);
        check("prim 64-bit total on deep chain", Prim.run(n, path).cost == 99999L * 1_000_000_000L);
        int[][] neg = new int[n - 1][];
        for (int i = 0; i < n - 1; i++) neg[i] = new int[] { i, i + 1, -1_000_000_000 };
        check("negative 64-bit total", Kruskal.run(n, neg).cost == -99999L * 1_000_000_000L && Prim.run(n, neg).cost == -99999L * 1_000_000_000L);

        // full limits: n=100000, m=250000, random weights in the whole range
        int[][] big = randEdges(new Random(3), n, 250000, -1_000_000_000, 1_000_000_000, true);
        Kruskal.Result k = Kruskal.run(n, big), p = Prim.run(n, big);
        check("kruskal == prim at full limits (cost and components)", k.cost == p.cost && k.components == p.components);
        Dsu d = new Dsu(n);
        boolean forest = true;
        for (int id : p.edgeIds) forest &= d.union(big[id][0], big[id][1]);
        check("prim output is acyclic at full limits and spans all components", forest && d.components() == p.components);
    }

    static void sccTests() {
        int[][] e = { { 0, 1 }, { 1, 2 }, { 2, 0 }, { 2, 3 }, { 3, 4 }, { 4, 3 }, { 1, 2 } };
        Scc.Result r = Scc.run(5, e);
        check("scc hand case: {0,1,2} -> {3,4}, one condensation pair", validScc(5, e, r) && r.count == 2 && r.condensation.length == 1);
        check("scc empty graph", Scc.run(0, new int[0][]).count == 0);
        check("scc self loop only", Scc.run(1, new int[][] { { 0, 0 } }).count == 1);
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500 && ok; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] g = randEdges(rnd, n, rnd.nextInt(16), 0, 0, false);
            ok = validScc(n, g, Scc.run(n, g));
        }
        check("scc 500 random graphs vs transitive closure", ok);
        // validator must reject bad witnesses
        int[][] two = { { 0, 1 }, { 1, 0 } };
        check("scc validator rejects split labels", !validScc(2, two, new Scc.Result(new int[] { 0, 1 }, 2, new int[0][])));
        check("scc validator rejects duplicate condensation pair",
                !validScc(2, new int[][] { { 0, 1 }, { 0, 1 } }, new Scc.Result(new int[] { 0, 1 }, 2, new int[][] { { 0, 1 }, { 0, 1 } })));
        int n = 100000;
        int[][] chain = new int[n - 1][], ring = new int[n][];
        for (int i = 0; i < n - 1; i++) chain[i] = new int[] { i, i + 1 };
        for (int i = 0; i < n; i++) ring[i] = new int[] { i, (i + 1) % n };
        Scc.Result c = Scc.run(n, chain);
        check("scc deep chain n=100000: every vertex alone, n-1 condensation pairs", c.count == n && c.condensation.length == n - 1);
        check("scc deep ring n=100000: one component", Scc.run(n, ring).count == 1);
        Scc.Result big = Scc.run(n, randEdges(new Random(5), n, 250000, 0, 0, false));
        check("scc full limits runs (labels in range)", big.count >= 1 && big.count <= n);
    }

    static void topoTests() {
        int[][] diamond = { { 0, 1 }, { 0, 2 }, { 1, 3 }, { 2, 3 } };
        Topo.Result r = Topo.run(4, diamond);
        check("topo diamond (non-unique order accepted by validity)", validTopo(4, diamond, r));
        check("topo validator accepts alternative order [0,1,2,3]", validTopo(4, diamond, new Topo.Result(true, new int[] { 0, 1, 2, 3 })));
        check("topo validator rejects invalid order [1,0,2,3]", !validTopo(4, diamond, new Topo.Result(true, new int[] { 1, 0, 2, 3 })));
        check("topo validator rejects 'acyclic' on cyclic graph", !validTopo(2, new int[][] { { 0, 1 }, { 1, 0 } }, new Topo.Result(true, new int[] { 0, 1 })));
        check("topo n=0", Topo.run(0, new int[0][]).acyclic);
        Topo.Result loop = Topo.run(1, new int[][] { { 0, 0 } });
        check("topo self-loop is a cycle, empty order", !loop.acyclic && loop.order.length == 0);
        Random rnd = new Random(6);
        boolean ok = true;
        for (int t = 0; t < 500 && ok; t++) {
            int n = 1 + rnd.nextInt(8);
            int[][] g = randEdges(rnd, n, rnd.nextInt(12), 0, 0, false);
            ok = validTopo(n, g, Topo.run(n, g));
        }
        check("topo 500 random graphs vs transitive closure", ok);
        int n = 100000;
        int[][] chain = new int[n - 1][], ring = new int[n][];
        for (int i = 0; i < n - 1; i++) chain[i] = new int[] { n - 1 - i, n - 2 - i }; // n-1 -> n-2 -> ... -> 0
        for (int i = 0; i < n; i++) ring[i] = new int[] { i, (i + 1) % n };
        Topo.Result c = Topo.run(n, chain);
        boolean exact = c.acyclic && c.order.length == n;
        for (int i = 0; exact && i < n; i++) exact = c.order[i] == n - 1 - i;
        check("topo deep chain n=100000 (no stack overflow, unique order)", exact);
        Topo.Result rr = Topo.run(n, ring);
        check("topo deep ring n=100000 -> false, empty order", !rr.acyclic && rr.order.length == 0);
    }

    static void dagTests() {
        int[][] e = { { 0, 1, 2 }, { 0, 1, 2 }, { 1, 2, -1 }, { 0, 2, 1 }, { 3, 4, 7 } };
        DagShortestPath.Result r = DagShortestPath.run(5, e, 0);
        check("dag hand case distances [0,2,1,null,null]",
                validDag(5, e, 0, r) && r.distance[1] == 2 && r.distance[2] == 1 && !r.reachable[3] && !r.reachable[4]);
        check("dag parent_edge = [-1,0,3,-1,-1] (ties keep the first edge)", Arrays.equals(r.parentEdge, new int[] { -1, 0, 3, -1, -1 }));
        check("route to reachable target 2 -> edges [3]", Arrays.equals(DagShortestPath.routeEdges(0, 2, r.parentEdge, e), new int[] { 3 }));
        check("route to reachable target 2 -> vertices [0,2]", Arrays.equals(DagShortestPath.routeVertices(0, 2, r.parentEdge, e), new int[] { 0, 2 }));
        check("route to source -> empty edge list, vertices [0]",
                DagShortestPath.routeEdges(0, 0, r.parentEdge, e).length == 0 && Arrays.equals(DagShortestPath.routeVertices(0, 0, r.parentEdge, e), new int[] { 0 }));
        check("route to unreachable target 4 -> null", DagShortestPath.routeEdges(0, 4, r.parentEdge, e) == null);
        check("route over parallel tied edges 0->1 picks edge 0 (not 1)", Arrays.equals(DagShortestPath.routeEdges(0, 1, r.parentEdge, e), new int[] { 0 }));
        int[][] cyc = { { 0, 1, 1 }, { 2, 3, 1 }, { 3, 2, 1 } };
        DagShortestPath.Result c = DagShortestPath.run(4, cyc, 0);
        check("dag cycle unreachable from source still -> acyclic=false, empty arrays", validDag(4, cyc, 0, c) && !c.acyclic);
        check("dag n=1 source only", DagShortestPath.run(1, new int[0][], 0).distance[0] == 0);
        check("dag self-loop at source -> false", !DagShortestPath.run(1, new int[][] { { 0, 0, 5 } }, 0).acyclic);
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 400 && ok; t++) {
            int n = 2 + rnd.nextInt(7);
            int[][] g = rnd.nextInt(5) == 0 ? randEdges(rnd, n, rnd.nextInt(12), -9, 9, true) : randDag(rnd, n, rnd.nextInt(14), -9, 9);
            int s = rnd.nextInt(n);
            ok = validDag(n, g, s, DagShortestPath.run(n, g, s));
        }
        check("dag 400 random graphs (DAGs + some cyclic) vs Bellman-Ford with negative weights", ok);
        // alternative valid witness: vertex 2 via edge 2 (0->1->2 = 2 + (-1) = 1) ties with direct edge 3
        DagShortestPath.Result alt = new DagShortestPath.Result(true, r.distance.clone(), r.reachable.clone(), new int[] { -1, 1, 2, -1, -1 });
        check("dag validator accepts alternative tied witness (edge 1 and edge 2)", validDag(5, e, 0, alt));
        // invalid witness: parent edge 0 ends at vertex 1, not at vertex 2
        DagShortestPath.Result bad = new DagShortestPath.Result(true, r.distance.clone(), r.reachable.clone(), new int[] { -1, 0, 0, -1, -1 });
        check("dag validator rejects parent edge that does not end at the vertex", !validDag(5, e, 0, bad));
        DagShortestPath.Result bad2 = new DagShortestPath.Result(true, r.distance.clone(), r.reachable.clone(), new int[] { -1, 0, 2, 0, -1 });
        check("dag validator rejects parent edge for an unreachable vertex", !validDag(5, e, 0, bad2));
        int n = 100000;
        int[][] chain = new int[n - 1][];
        for (int i = 0; i < n - 1; i++) chain[i] = new int[] { i, i + 1, 1 };
        DagShortestPath.Result d = DagShortestPath.run(n, chain, 0);
        int[] route = DagShortestPath.routeEdges(0, n - 1, d.parentEdge, chain);
        check("dag deep chain n=100000: distance n-1, route has n-1 edges", d.distance[n - 1] == n - 1 && route.length == n - 1 && route[0] == 0 && route[n - 2] == n - 2);
        int[][] ring = new int[n][];
        for (int i = 0; i < n; i++) ring[i] = new int[] { i, (i + 1) % n, 1 };
        check("dag deep ring -> false", !DagShortestPath.run(n, ring, 0).acyclic);
        int[][] bigDag = randDag(new Random(8), n, 250000, -1_000_000_000, 1_000_000_000);
        check("dag full limits runs and is acyclic", DagShortestPath.run(n, bigDag, 0).acyclic);
    }

    static void mainTests() {
        String[] reqs = {
            "{\"task\":\"ping\"}",
            "{\"task\":\"dsu\",\"n\":5,\"operations\":[[0,1],[1,0],[2,3],[1,3],[4,4]]}",
            "{\"task\":\"kruskal\",\"n\":3,\"edges\":[[0,1,5],[1,2,-2]]}",
            "{\"task\":\"dag\",\"n\":3,\"source\":0,\"edges\":[[0,1,1],[1,2,1]]}",
            "{\"task\":\"topo\",\"n\":0,\"edges\":[]}",
        };
        String[] first = new String[reqs.length];
        for (int i = 0; i < reqs.length; i++) first[i] = Main.handle(reqs[i]);
        boolean same = true;
        for (int round = 0; round < 3; round++)
            for (int i = reqs.length - 1; i >= 0; i--) same &= first[i].equals(Main.handle(reqs[i])); // reversed order, repeated
        check("repeated mixed requests give identical results (state reset, ping preserved)", same);
        check("ping answered", first[0].contains("\"ok\":true"));
        check("dsu exact output", first[1].equals("{\"task\":\"dsu\",\"states\":[[4,2],[4,2],[3,2],[2,4],[2,4]]}"));
        check("kruskal exact output", first[2].equals("{\"task\":\"kruskal\",\"cost\":3,\"edge_ids\":[1,0],\"components\":1}"));
        check("dag exact output", first[3].equals("{\"task\":\"dag\",\"acyclic\":true,\"distance\":[0,1,2],\"parent_edge\":[-1,0,1]}"));
        check("dag unreachable -> null", Main.handle("{\"task\":\"dag\",\"n\":2,\"source\":0,\"edges\":[]}").contains("\"distance\":[0,null],\"parent_edge\":[-1,-1]"));
        check("dag cycle -> two empty arrays", Main.handle("{\"task\":\"dag\",\"n\":2,\"source\":0,\"edges\":[[1,1,1]]}").contains("\"acyclic\":false,\"distance\":[],\"parent_edge\":[]"));
        check("bad JSON gives an error line, not a crash", Main.handle("{oops").contains("\"error\""));
        // full-limit request through the JSON layer
        Random rnd = new Random(9);
        int[][] e = randEdges(rnd, 100000, 250000, -1_000_000_000, 1_000_000_000, true);
        String a = Main.handle(json("kruskal", 100000, "edges", e, ""));
        String b = Main.handle(json("prim", 100000, "edges", e, ""));
        check("JSON layer at full limits: kruskal and prim report the same cost",
                a.substring(a.indexOf("\"cost\""), a.indexOf(",\"edge_ids\"")).equals(b.substring(b.indexOf("\"cost\""), b.indexOf(",\"edge_ids\""))));
    }

    public static void main(String[] args) {
        dsuTests();
        mstTests();
        sccTests();
        topoTests();
        dagTests();
        mainTests();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
