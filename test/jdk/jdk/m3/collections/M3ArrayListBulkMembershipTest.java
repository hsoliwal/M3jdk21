/*
 * @test
 * @summary M3ArrayList array-owned bulk membership preserves List semantics
 * @modules jdk.m3.collections
 * @run main M3ArrayListBulkMembershipTest
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import jdk.m3.collections.M3ArrayList;

public class M3ArrayListBulkMembershipTest {
    static long checks;
    static void check(boolean yes, String what) {
        checks++;
        if (!yes) throw new AssertionError(what);
    }

    static void same(ArrayList<Object> expected, M3ArrayList<Object> actual) {
        check(expected.size() == actual.size(), "size");
        for (int i = 0; i < expected.size(); i++) {
            check(expected.get(i) == actual.get(i), "identity at " + i);
        }
    }

    static ArrayList<Object> values(int size, int seed) {
        Random random = new Random(0x51cabeL + size * 17L + seed);
        ArrayList<Object> data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int n = random.nextInt(101);
            data.add(n % 13 == 0 ? null : n % 3 == 0 ? "key-" + n
                    : n % 3 == 1 ? (Object) Integer.valueOf(n) : Long.valueOf(n));
        }
        return data;
    }

    static void scenario(ArrayList<Object> originals, ArrayList<Object> members, boolean retain) {
        ArrayList<Object> expected = new ArrayList<>(originals);
        M3ArrayList<Object> actual = new M3ArrayList<>(originals);
        boolean expectedChange = retain ? expected.retainAll(members) : expected.removeAll(members);
        boolean actualChange = retain ? actual.retainAll(members) : actual.removeAll(members);
        check(expectedChange == actualChange, "changed flag");
        same(expected, actual);
        check(actual.capacity() >= actual.size(), "capacity invariant");
    }

    static void boundaries() {
        int[] sizes = {0, 1, 31, 511, 8192, 16383, 16384, 16401};
        int[] counts = {0, 1, 31, 511, 512, 1024};
        for (int size : sizes) {
            for (int count : counts) {
                for (int seed = 0; seed < 3; seed++) {
                    ArrayList<Object> original = values(size, seed);
                    ArrayList<Object> members = values(count, 49 + seed);
                    for (int j = 0; j < Math.min(4, Math.min(size, count)); j++) {
                        members.set(j, original.get(j));
                    }
                    scenario(original, members, false);
                    scenario(original, members, true);
                }
            }
        }
    }

    static final class Probe {
        static int equalsCalls;
        static int hashCalls;
        final int id;
        Probe(int id) { this.id = id; }
        @Override public boolean equals(Object value) {
            equalsCalls++;
            return value instanceof Probe probe && id == probe.id;
        }
        @Override public int hashCode() {
            hashCalls++;
            return id;
        }
    }

    static void opaqueFallback() {
        ArrayList<Object> original = new ArrayList<>();
        ArrayList<Object> members = new ArrayList<>();
        for (int i = 0; i < 8192; i++) original.add(new Probe(i % 83));
        for (int i = 0; i < 512; i++) members.add(new Probe(i % 83));
        for (boolean retain : new boolean[] {true, false}) {
            ArrayList<Object> expected = new ArrayList<>(original);
            M3ArrayList<Object> actual = new M3ArrayList<>(original);
            Probe.equalsCalls = Probe.hashCalls = 0;
            boolean changed1 = retain ? expected.retainAll(members) : expected.removeAll(members);
            int eq = Probe.equalsCalls;
            int hash = Probe.hashCalls;
            Probe.equalsCalls = Probe.hashCalls = 0;
            boolean changed2 = retain ? actual.retainAll(members) : actual.removeAll(members);
            check(changed1 == changed2, "opaque changed");
            same(expected, actual);
            check(Probe.equalsCalls == eq, "opaque equals callback order/count");
            check(Probe.hashCalls == hash, "opaque hash callback count");
        }
    }

    static void iteratorAndExceptions() {
        M3ArrayList<String> data = new M3ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> iter = data.iterator();
        check(data.removeAll(List.of("a")), "single remove");
        boolean concurrent = false;
        try { iter.next(); } catch (ConcurrentModificationException expected) {
            concurrent = true;
        }
        check(concurrent, "fail-fast");
        boolean npe = false;
        try { data.retainAll(null); } catch (NullPointerException expected) { npe = true; }
        check(npe, "retain null NPE");
        npe = false;
        try { data.removeAll(null); } catch (NullPointerException expected) { npe = true; }
        check(npe, "remove null NPE");
        M3ArrayList<String> empty = new M3ArrayList<>();
        check(!empty.removeAll(List.of()), "empty remove");
        check(!empty.retainAll(List.of()), "empty retain");
    }

    public static void main(String[] args) {
        boundaries();
        opaqueFallback();
        iteratorAndExceptions();
        System.out.println("M3ArrayListBulkMembershipTest PASS checks=" + checks);
    }
}
