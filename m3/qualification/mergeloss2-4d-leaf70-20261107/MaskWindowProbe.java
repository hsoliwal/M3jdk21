import java.util.Arrays;
import java.util.SplittableRandom;
import jdk.internal.m3.M3IndexMaskWindow;
import jdk.internal.m3.M3IndexMaskWindow.Backend;

/**
 * MaskWindowProbe : MASK_WINDOW_DIFFERENTIAL v1 (Java backend). scan / scanBounded over random rows, unsigned byte states,
 * masks (incl. overlapping and zero), cursors, budgets, limits and output offsets; refusals are recorded by type. The
 * scan digest is compared between the master and the leaf class. With "count", countBounded(JAVA) is also checked against
 * a reference model (count of states in [first, first+min(budget, n-first)) with all required and no excluded bits).
 */
public final class MaskWindowProbe {
    public static void main(String[] a) {
        SplittableRandom r = new SplittableRandom(Long.parseLong(a[0]));
        int rounds = Integer.parseInt(a[1]);
        boolean count = a.length > 2;
        long digest = 3, checks = 0, countChecks = 0, countBad = 0;
        for (int round = 0; round < rounds; round++) {
            int n = r.nextInt(80);
            int[] rows = new int[n];
            byte[] states = new byte[n];
            for (int i = 0; i < n; i++) { rows[i] = r.nextInt(1000) - 3; states[i] = (byte) r.nextInt(256); }
            int req = r.nextInt(4) == 0 ? 0 : r.nextInt(256) & r.nextInt(256), exc = r.nextInt(3) == 0 ? 0 : r.nextInt(256) & r.nextInt(256) & r.nextInt(256);
            if (r.nextInt(30) == 0) req = 300;
            int first = r.nextInt(n + 3) - 1, budget = r.nextInt(n + 5) - (r.nextInt(25) == 0 ? 3 : 0);
            int[] out = new int[r.nextInt(20)];
            int off = r.nextInt(out.length + 2) - (r.nextInt(25) == 0 ? 1 : 0), limit = r.nextInt(25) - (r.nextInt(30) == 0 ? 2 : 0);
            final int fq = first, fr = req, fe = exc, fo = off, fl = limit, fb = budget;
            int[] o1 = out.clone(), o2 = out.clone();
            digest = digest * 31 + call(() -> M3IndexMaskWindow.scan(rows, states, fq, fr, fe, o1, fo, fl, Backend.JAVA)).hashCode();
            digest = digest * 31 + Arrays.hashCode(o1);
            digest = digest * 31 + call(() -> M3IndexMaskWindow.scanBounded(rows, states, fq, fb, fr, fe, o2, fo, fl, Backend.JAVA)).hashCode();
            digest = digest * 31 + Arrays.hashCode(o2);
            checks += 2;
            if (count) {
                String got = callCount(states, fq, fb, fr, fe);
                String want;
                if (fr < 0 || fr > 255 || fe < 0 || fe > 255) want = "MASK";
                else if (fq < 0 || fq > n) want = "THROW IndexOutOfBoundsException";
                else if (fb < 0) want = "THROW IllegalArgumentException";
                else {
                    int end = fq + Math.min(fb, n - fq), c = 0;
                    for (int i = fq; i < end; i++) { int s = states[i] & 0xff; if ((s & fr) == fr && (s & fe) == 0) c++; }
                    want = end + ":" + c;
                }
                countChecks++;
                if (!(want.equals(got) || (want.equals("MASK") && got.startsWith("THROW")))) { if (countBad++ < 5) System.out.println("COUNT MISMATCH want=" + want + " got=" + got); }
            }
        }
        System.out.println("MASK_WINDOW_DIFFERENTIAL checks=" + checks + " digest=" + Long.toHexString(digest)
                + (count ? " count_checks=" + countChecks + " count_mismatches=" + countBad : ""));
    }

    interface Op { long run(); }

    static String call(Op op) {
        try { long v = op.run(); return M3IndexMaskWindow.nextIndex(v) + ":" + M3IndexMaskWindow.written(v); }
        catch (RuntimeException e) { return "THROW " + e.getClass().getSimpleName(); }
    }

    static String callCount(byte[] s, int f, int b, int req, int exc) {
        try {
            java.lang.reflect.Method m = M3IndexMaskWindow.class.getMethod("countBounded", byte[].class, int.class, int.class, int.class, int.class, Backend.class);
            long v = (long) m.invoke(null, s, f, b, req, exc, Backend.JAVA);
            return M3IndexMaskWindow.nextIndex(v) + ":" + M3IndexMaskWindow.written(v);
        } catch (java.lang.reflect.InvocationTargetException e) { return "THROW " + e.getCause().getClass().getSimpleName(); }
        catch (ReflectiveOperationException e) { return "NO_METHOD"; }
    }
}
