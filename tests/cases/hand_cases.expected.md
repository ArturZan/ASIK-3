# Hand cases: inputs in hand_cases.jsonl, one result per line in hand_cases.actual.jsonl

Where several answers are valid, the "expected" column describes the property that must hold, not one literal output.

| # | Request | Expected (with justification) |
|---|---------|-------------------------------|
| 1 | ping | answered, nothing else printed |
| 2 | dsu n=5 | 5 comps/largest 1; (0,1) -> [4,2]; (1,0) already joined -> [4,2]; (2,3) -> [3,2]; (1,3) merges {0,1},{2,3} -> [2,4]; (4,4) loop -> [2,4]. Result `[[4,2],[4,2],[3,2],[2,4],[2,4]]` (unique) |
| 3 | kruskal | Loop (id 3) and the parallel/cycle-closing edges ids 0, 2 are never used. Cheapest spanning edges: id 1 (-3) and id 4 (2), cost -1. Components: {0,1,2},{3},{4} = 3. edge_ids = {1,4} (the only optimal forest) |
| 4 | prim | Same graph, same cost -1, components 3, edge_ids as a set = {1,4}; order may differ from Kruskal (Prim picks 4 then 1) |
| 5 | scc | {0,1,2} is a cycle, {3,4} is a cycle, 2->3 is the only cross edge: 2 components, exactly one condensation pair from comp(0) to comp(3) (the duplicate edge 1->2 is inside a component) |
| 6 | topo diamond | Any order with 0 first, 3 last, 1 and 2 in either order: [0,1,2,3] and [0,2,1,3] are both valid |
| 7 | topo cycle 1<->2 | acyclic=false, order=[] (DFS from 0 reaches 1, then 2, then sees gray vertex 1 again: back edge 2->1) |
| 8 | dag, ties | dist [0,2,1,null,null]. Vertex 1: two parallel edges tie at 2, first one (id 0) kept. Vertex 2: direct edge id 3 gives 1, via 1 gives 2-1=1, tie, id 3 is relaxed first and kept. parent_edge [-1,0,3,-1,-1]. 3, 4 unreachable -> null/-1 |
| 9 | dag, cycle unreachable from source | cycle 2<->3 is not reachable from 0, but the whole graph is checked: acyclic=false, distance=[], parent_edge=[] |
| 10 | kruskal n=0 | cost 0, edge_ids [], components 0 |
| 11 | same dsu request as #2, repeated later | must equal #2 exactly (no state carried between requests) |
