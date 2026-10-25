import java.util.*;

/**
 * Kruskal vs Prim on IDENTICAL graphs at three sizes.
 * Timer: System.nanoTime(). Timed region: only Kruskal.run / Prim.run (graph already generated and held in memory,
 * no JSON parsing, no output). Warm-up: 3 untimed runs per method per size (JIT). Timings: 7 per method per size, median reported.
 * Java compilation is not part of the measurement (classes are compiled beforehand).
 */
public final class Benchmark {
    static final long SEED = 20260101L;
    static final int WARMUP = 3, RUNS = 7;

    public static void main(String[] args) {
        int[][] sizes = { { 1_000, 5_000 }, { 10_000, 50_000 }, { 100_000, 250_000 } };
        System.out.println("seed=" + SEED + " warmup=" + WARMUP + " runs=" + RUNS + " weights in [-1e6, 1e6]");
        System.out.println("java=" + System.getProperty("java.version") + " vm=" + System.getProperty("java.vm.name")
                + " os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " cpus=" + Runtime.getRuntime().availableProcessors());
        System.out.printf("%-10s %-9s %-7s %-12s %-12s%n", "n", "m", "method", "median_ms", "all_ms");
        for (int[] sz : sizes) {
            int n = sz[0], m = sz[1];
            Random rnd = new Random(SEED + n);
            int[][] edges = new int[m][];
            for (int i = 0; i < m; i++) edges[i] = new int[] { rnd.nextInt(n), rnd.nextInt(n), rnd.nextInt(2_000_001) - 1_000_000 };
            long ck = 0, cp = 0;
            for (int i = 0; i < WARMUP; i++) { ck = Kruskal.run(n, edges).cost; cp = Prim.run(n, edges).cost; }
            if (ck != cp) throw new IllegalStateException("costs differ: " + ck + " vs " + cp);
            report(n, m, "kruskal", time(() -> Kruskal.run(n, edges)));
            report(n, m, "prim", time(() -> Prim.run(n, edges)));
            System.out.println("          (both methods: cost=" + ck + ")");
        }
    }

    static double[] time(Runnable r) {
        double[] t = new double[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long s = System.nanoTime();
            r.run();
            t[i] = (System.nanoTime() - s) / 1e6;
        }
        return t;
    }

    static void report(int n, int m, String name, double[] t) {
        double[] sorted = t.clone();
        Arrays.sort(sorted);
        StringBuilder all = new StringBuilder();
        for (double x : t) all.append(String.format("%.2f ", x));
        System.out.printf("%-10d %-9d %-7s %-12.2f %s%n", n, m, name, sorted[RUNS / 2], all.toString().trim());
    }
}
