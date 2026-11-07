import java.util.Arrays;
import java.util.stream.Gatherer;
import java.util.stream.Stream;
public class GatherSize {
    public static void main(String[] a) {
        Gatherer<Integer, ?, Integer> twice = Gatherer.of((s, e, d) -> d.push(e) && d.push(e));
        Gatherer<Integer, ?, Integer> odd = Gatherer.of((s, e, d) -> e % 2 == 0 || d.push(e));
        String r1, r2;
        try { r1 = Arrays.toString(Stream.of(1, 2, 3).gather(twice).toArray()); } catch (RuntimeException ex) { r1 = "THROW " + ex; }
        try { r2 = Arrays.toString(Stream.of(1, 2, 3, 4, 5).gather(odd).toArray()); } catch (RuntimeException ex) { r2 = "THROW " + ex; }
        String r3; try { r3 = Stream.of(3, 1, 2).gather(twice).sorted().toList().toString(); } catch (RuntimeException ex) { r3 = "THROW " + ex; }
        System.out.println("sortedExpand=" + r3);
        System.out.println("expand=" + r1 + " shrink=" + r2);
    }
}
