import java.util.Arrays;

/** Minimal binary min-heap of primitive longs. */
final class LongHeap {
    private long[] a = new long[16];
    private int size;

    boolean isEmpty() { return size == 0; }

    void push(long x) {
        if (size == a.length) a = Arrays.copyOf(a, size * 2);
        int i = size++;
        while (i > 0) {
            int p = (i - 1) >> 1;
            if (a[p] <= x) break;
            a[i] = a[p];
            i = p;
        }
        a[i] = x;
    }

    long pop() {
        long top = a[0];
        long x = a[--size];
        int i = 0;
        while (true) {
            int c = 2 * i + 1;
            if (c >= size) break;
            if (c + 1 < size && a[c + 1] < a[c]) c++;
            if (a[c] >= x) break;
            a[i] = a[c];
            i = c;
        }
        if (size > 0) a[i] = x;
        return top;
    }
}
