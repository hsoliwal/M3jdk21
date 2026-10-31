import com.m3.arrays.M3Arrays;

/** Regression witness: four immutable joins accept empty components but cannot read index zero. */
public final class M3EmptySegmentWitness {
    private static int failures;
    public static void main(String[] args) {
        check("byte", () -> M3Arrays.join(M3Arrays.bytes(new byte[] {11}),
                M3Arrays.bytes(new byte[0]), M3Arrays.bytes(new byte[0]),
                M3Arrays.bytes(new byte[] {22})).byteAt(0), 11);
        check("char", () -> M3Arrays.join(M3Arrays.chars(new char[] {'A'}),
                M3Arrays.chars(new char[0]), M3Arrays.chars(new char[0]),
                M3Arrays.chars(new char[] {'B'})).charAt(0), 'A');
        check("int", () -> M3Arrays.join(M3Arrays.ints(new int[] {11}),
                M3Arrays.ints(new int[0]), M3Arrays.ints(new int[0]),
                M3Arrays.ints(new int[] {22})).intAt(0), 11);
        check("long", () -> M3Arrays.join(M3Arrays.longs(new long[] {11}),
                M3Arrays.longs(new long[0]), M3Arrays.longs(new long[0]),
                M3Arrays.longs(new long[] {22})).longAt(0), 11);
        System.out.println("incorrect joined reads=" + failures + "/4");
        if (failures != 4) throw new AssertionError("baseline witness changed");
    }

    private static void check(String family, java.util.function.LongSupplier read, long expected) {
        try {
            long actual = read.getAsLong();
            if (actual == expected) throw new AssertionError("expected current-source defect for " + family);
            failures++;
            System.out.println(family + ": incorrect value=" + actual);
        } catch (IndexOutOfBoundsException defect) {
            failures++;
            System.out.println(family + ": " + defect.getClass().getName());
        }
    }
}
