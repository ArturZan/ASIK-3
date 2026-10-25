import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * JSON Lines adapter: one request per input line, exactly one result line per request on stdout.
 * No state is kept between requests (every call builds its own structures), so the "reset" is structural.
 */
public final class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8), 1 << 16);
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), false, StandardCharsets.UTF_8);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) continue;
            out.print(handle(line));
            out.print('\n');
            out.flush();
        }
    }

    @SuppressWarnings("unchecked")
    public static String handle(String line) {
        String task = "?";
        try {
            Map<String, Object> req = (Map<String, Object>) Json.parse(line);
            task = String.valueOf(req.get("task"));
            int n = req.get("n") == null ? 0 : ((Number) req.get("n")).intValue();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"task\":").append(Json.quote(task));
            switch (task) {
                case "ping" -> sb.append(",\"ok\":true");
                case "dsu" -> {
                    int[][] states = Dsu.run(n, matrix(req.get("operations")));
                    sb.append(",\"states\":");
                    matrix(sb, states);
                }
                case "kruskal", "prim" -> {
                    int[][] edges = matrix(req.get("edges"));
                    Kruskal.Result r = task.equals("kruskal") ? Kruskal.run(n, edges) : Prim.run(n, edges);
                    sb.append(",\"cost\":").append(r.cost).append(",\"edge_ids\":");
                    ints(sb, r.edgeIds);
                    sb.append(",\"components\":").append(r.components);
                }
                case "scc" -> {
                    Scc.Result r = Scc.run(n, matrix(req.get("edges")));
                    sb.append(",\"component\":");
                    ints(sb, r.component);
                    sb.append(",\"condensation\":");
                    matrix(sb, r.condensation);
                }
                case "topo" -> {
                    Topo.Result r = Topo.run(n, matrix(req.get("edges")));
                    sb.append(",\"acyclic\":").append(r.acyclic).append(",\"order\":");
                    ints(sb, r.order);
                }
                case "dag" -> {
                    int source = ((Number) req.get("source")).intValue();
                    DagShortestPath.Result r = DagShortestPath.run(n, matrix(req.get("edges")), source);
                    sb.append(",\"acyclic\":").append(r.acyclic).append(",\"distance\":[");
                    for (int i = 0; i < r.distance.length; i++) {
                        if (i > 0) sb.append(',');
                        if (r.reachable[i]) sb.append(r.distance[i]); else sb.append("null");
                    }
                    sb.append("],\"parent_edge\":");
                    ints(sb, r.parentEdge);
                }
                default -> throw new IllegalArgumentException("unknown task");
            }
            return sb.append('}').toString();
        } catch (RuntimeException e) {
            return "{\"task\":" + Json.quote(task) + ",\"error\":" + Json.quote(String.valueOf(e.getMessage())) + "}";
        }
    }

    private static int[][] matrix(Object o) {
        if (o == null) return new int[0][];
        List<?> rows = (List<?>) o;
        int[][] m = new int[rows.size()][];
        for (int i = 0; i < m.length; i++) {
            List<?> r = (List<?>) rows.get(i);
            int[] row = new int[r.size()];
            for (int j = 0; j < row.length; j++) row[j] = ((Number) r.get(j)).intValue();
            m[i] = row;
        }
        return m;
    }

    private static void ints(StringBuilder sb, int[] a) {
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        sb.append(']');
    }

    private static void matrix(StringBuilder sb, int[][] m) {
        sb.append('[');
        for (int i = 0; i < m.length; i++) {
            if (i > 0) sb.append(',');
            ints(sb, m[i]);
        }
        sb.append(']');
    }
}
