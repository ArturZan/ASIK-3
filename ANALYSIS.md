# Assignment 3 – Graph Algorithms (Java 17)

## Build, run, test

```bash
javac --release 17 -d out src/*.java                      # build (Java 17 language level)
java -cp out Main < tests/cases/hand_cases.jsonl          # run: JSON Lines in, one JSON line out per request
javac --release 17 -d out src/*.java tests/*.java         # build with tests
java -cp out AlgoTests                                    # own tests (exit code 1 on failure)
java -cp out Benchmark                                    # Kruskal vs Prim benchmark
```

Layout: `src/` (`Dsu`, `Kruskal`, `Prim`, `Scc`, `Topo`, `DagShortestPath`, helpers `Csr`, `LongHeap`, and the
JSON Lines adapter `Main`/`Json`), `tests/` (`AlgoTests`, `Benchmark`, `cases/`).
Algorithms are implemented by hand; only `Arrays.sort` and plain arrays are used from the standard library.

## Interface and state

`Main` reads one JSON object per line and prints exactly one JSON object per line (nothing else on stdout).
`ping` is answered with `{"task":"ping","ok":true}`. A malformed request produces `{"task":...,"error":...}`
instead of crashing. **State reset:** there is no static or shared mutable state. Every request builds its own
arrays (`Dsu`, `Csr`, visited arrays, heap), so a request cannot see data from the previous one; the test
`repeated mixed requests ...` runs five task types repeatedly in forward and reversed order and requires identical output.

**Deep graphs:** every DFS (`Scc` both passes, `Topo`) uses an explicit `int[]` stack and a per-vertex edge pointer,
never recursion, so a chain of 100 000 vertices cannot overflow the Java call stack. `Dsu.find` is iterative too.

All weights fit `int` (|w| ≤ 10⁹), but every total (MST cost, distances) is accumulated in `long`
(maximum |total| ≈ 10⁵·10⁹ = 10¹⁴).

## dsu — `Dsu.run`

* **Steps:** `parent[i]=i`, `size[i]=1`, `components=n`, `largest=1`. For each pair: `find` both roots (two-pass path
  compression), if different attach the smaller tree under the larger, add sizes, `components--`, update `largest`.
  Record `[components, largest]` after every operation, including no-op unions.
* **Invariants:** every set is a tree rooted at a vertex with `parent[r]=r`; `size[r]` is the number of vertices in
  the tree of `r`; `components` = number of roots; `largest` = max over all sizes (sizes only grow, so a running max is exact).
* **Correctness:** `find` returns the unique root; union merges exactly two sets, so the three counters stay consistent.
* **Boundaries:** `n=0` with no operations, loops `(u,u)`, repeated pairs (state unchanged, still recorded).
* **Complexity:** O((n + q)·α(n)) time, O(n + q) memory (q = operations; the output has q rows).

## kruskal — `Kruskal.run`

* **Steps:** encode each edge as one `long` `((w + 10⁹) << 20) | id`, sort the array, scan in order and take an edge
  when `union` of its endpoints succeeds; stop after `n-1` edges. `components = n - taken`.
* **Invariant:** the chosen edges form a forest that is contained in some minimum spanning forest (cut/cycle property:
  the cheapest edge that connects two different trees is safe).
* **Correctness:** loops and edges inside one tree fail `union` and are skipped; negative weights need no special case
  (the shift only makes the key non-negative); ties are broken by edge id, which is deterministic; isolates simply stay as singleton components.
  Output ids are the original positions in `edges`.
* **Complexity:** O(m log m + m·α(n)) time, O(n + m) memory.

## prim — `Prim.run`

* **Steps:** build undirected CSR adjacency. For every unvisited vertex `s` (restart, so a forest and a component count
  are produced): mark visited, push its edges to unvisited neighbours as `(weight, id)` keys in a primitive binary heap.
  Pop the cheapest; if both endpoints are visited it is stale (or a loop) and is dropped; otherwise visit the new endpoint,
  add the edge and cost, push the new vertex's edges. The heap is empty exactly when the component is complete.
* **Invariant:** the visited set of the current component is connected through the chosen edges, and the next chosen
  edge is a cheapest edge leaving it (cut property).
* **Correctness:** each component is spanned by its own tree, `components` counts the restarts, isolates cost nothing and count as components.
  Lazy deletion is safe because a stale entry can only connect two already visited vertices.
* **Complexity:** O(m log m) time (each of ≤ 2m heap entries costs O(log m)), O(n + m) memory.

## scc — `Scc.run` (Kosaraju)

* **Steps:** (1) iterative DFS on G over all vertices, record vertices in finishing order; (2) build the transpose (reversed
  CSR); (3) process vertices in decreasing finishing time, each unlabelled vertex starts a DFS on the transpose and the
  vertices it reaches get the next label; (4) for every edge `u→v` with different labels emit key `label[u]*k + label[v]`,
  sort, drop duplicates, decode into pairs.
* **Invariant:** the vertex with the largest finishing time in the unprocessed part belongs to a source component of the
  condensation of the remaining graph, so its transpose-DFS cannot leave its own component.
* **Correctness:** standard Kosaraju argument; labels are `0..k-1`; `condensation` contains every distinct cross pair exactly
  once and never a pair `(c,c)` (edges inside a component and self-loops are filtered).
* **Complexity:** O(n + m) for the DFS passes plus O(m log m) for sorting condensation keys; memory O(n + m).

## topo — `Topo.run`

* **Steps:** colours white/gray/black. Iterative DFS from every white vertex; reaching a gray vertex means a back edge,
  i.e. a directed cycle (a self-loop is the smallest case) → return `acyclic=false`, `order=[]`. Otherwise record
  post-order; the topological order is the reverse post-order.
* **Invariant:** the gray vertices are exactly the current DFS stack, which is a path; a vertex turns black only after all
  its successors are black, so it appears after them in post-order.
* **Correctness:** with no back edge, every edge `u→v` has `v` finished before `u`, so `u` precedes `v` in the reversed order.
  Isolated vertices and disconnected parts are covered because the loop starts DFS from every vertex.
* **Complexity:** O(n + m) time and memory.

## dag — `DagShortestPath.run` and route helper

* **Steps:** run `Topo` on the **whole** graph (so a cycle anywhere, including a part unreachable from `source`, gives
  `acyclic=false` with empty `distance`/`parent_edge`). Otherwise `dist[source]=0`, and in topological order relax every outgoing edge
  of every reachable vertex. A strict `<` is used, so on ties the edge relaxed first is kept.
  Output: unreachable → `distance=null`, `parent_edge=-1`; source → `0` and `-1`; others → original edge id.
* **Invariant:** when vertex `u` is processed, all of its predecessors are already processed, so `dist[u]` is final.
* **Correctness:** negative weights are fine because every edge is relaxed exactly once in an order consistent with all
  edges (no relaxation can be invalidated later). Parallel edges are handled as separate edge ids.
* **Complexity:** O(n + m) time and memory.
* **Route helper:** `routeEdges(source, target, parentEdge, edges)` follows `parentEdge` from `target` back to `source` and reverses;
  `routeVertices(...)` returns the vertex list. `null` means unreachable (or a cyclic graph with empty arrays).

Calls on the graph `n=5, source=0, edges=[[0,1,2],[0,1,2],[1,2,-1],[0,2,1],[3,4,7]]`
(`parent_edge = [-1,0,3,-1,-1]`, `distance = [0,2,1,null,null]`):

| Call | Result | Why |
|------|--------|-----|
| `routeEdges(0, 2, ...)` | `[3]` | reachable target; the direct edge 3 (cost 1) ties with 0→1→2 (2 + (−1) = 1), edge 3 was relaxed first |
| `routeVertices(0, 2, ...)` | `[0, 2]` | same route as vertices |
| `routeEdges(0, 0, ...)` | `[]` | target is the source: empty route (vertices: `[0]`) |
| `routeEdges(0, 4, ...)` | `null` | unreachable target (`parent_edge[4] = -1`) |
| `routeEdges(0, 1, ...)` | `[0]` | parallel edges 0 and 1 tie at cost 2; the lower id wins |

## Tests (`tests/`)

`AlgoTests` (64 checks, all passing) covers: empty graph, `n=1`, loops, parallel edges, negative weights, isolates,
64-bit totals (path of 10⁵ vertices with weight 10⁹), full limits (`n=100000`, `m=250000`, weights over the whole range), deep chains and
rings of 10⁵ vertices, cycles unreachable from the DAG source, and repeated mixed requests through the JSON layer.
Non-unique answers are checked **by validity** against independent oracles: brute force over all edge subsets for MSF,
transitive closure for SCC/topo/cycle detection, Bellman-Ford for DAG distances. The validators themselves are tested:
they accept an alternative valid forest (`{0,2}` instead of `{0,1}`), an alternative topological order and an alternative tied
shortest-path witness, and they reject a cyclic "forest", a wrong cost, an invalid topological order, split SCC labels,
duplicated condensation pairs and invalid parent edges.
`tests/cases/hand_cases.jsonl` holds hand-made requests; `hand_cases.expected.md` justifies each expected answer and
`hand_cases.actual.jsonl` is the real output of `java -cp out Main < tests/cases/hand_cases.jsonl`.

## Benchmark: Kruskal vs Prim

* **Command:** `java -cp out Benchmark` (full output saved in `tests/benchmark_output.txt`).
* **Inputs:** random multigraphs (loops and parallel edges allowed), weights uniform in [−10⁶, 10⁶]; seed = `20260101 + n`;
  both methods receive the *same* `int[][] edges` object; sizes (n, m) = (1 000, 5 000), (10 000, 50 000), (100 000, 250 000).
* **Timer / boundary:** `System.nanoTime()` around a single `Kruskal.run` or `Prim.run` call. Graph generation, JSON parsing and
  output are outside the timed region; Prim's CSR construction is inside it (it is part of the algorithm). Compilation is excluded
  (classes built beforehand). **Warm-up:** 3 untimed runs per method per size. **Timings:** 7 per method per size; median reported.
* **Environment:** OpenJDK 21.0.12 (compiled with `--release 17`), Linux x86_64 VM, Intel Xeon @ 2.10 GHz, **1 CPU**, so single-run noise is visible.

| n | m | Kruskal median (ms) | Prim median (ms) | Kruskal timings (ms) | Prim timings (ms) |
|---|---|---|---|---|---|
| 1 000 | 5 000 | 0.69 | 1.17 | 0.69 0.73 0.69 0.66 4.77 0.66 0.65 | 1.17 1.11 4.80 1.17 1.11 1.12 3.19 |
| 10 000 | 50 000 | 11.13 | 30.75 | 14.26 9.88 11.29 11.13 9.45 16.79 4.32 | 31.80 24.23 29.06 31.39 31.46 28.86 30.75 |
| 100 000 | 250 000 | 24.39 | 81.97 | 36.83 24.39 23.86 23.83 47.56 26.36 24.04 | 81.97 80.15 79.18 79.47 84.74 83.69 85.78 |

Both methods returned the same cost on every graph (checked before timing). Kruskal was faster on all three sizes, by roughly
1.7×, 2.8× and 3.4×. Both are O(m log m); the plausible reason for the gap (not separately profiled) is the constants: Kruskal does one
primitive `long` sort plus near-constant DSU work, while Prim builds adjacency arrays and does up to 2m heap pushes/pops with
random memory access. The occasional outliers (e.g. 4.77 ms, 47.56 ms) come from JIT/GC/VM noise, which is why medians are reported.
These conclusions apply to sparse random graphs on this machine only.

## Sources and AI assistance

Algorithms follow the standard textbook treatments (Cormen et al., *Introduction to Algorithms*: DSU, Kruskal, Prim, SCC via Kosaraju,
DFS topological sort, DAG shortest paths). The code, tests and this document were produced with the help of an AI assistant (Claude by
Anthropic) and then compiled and run; **the author must review everything, and adjust this paragraph to the wording the syllabus requires.**
