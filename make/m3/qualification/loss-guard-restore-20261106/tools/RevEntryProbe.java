import java.util.Map;
import java.util.SequencedMap;
import java.util.TreeMap;
import java.util.Collections;
import java.util.SortedMap;

/** RevEntryProbe : Map.Entry.setValue contract on SortedMap.reversed() entries of a non-navigable map (ReverseOrderSortedMapView.ViewEntry). */
public final class RevEntryProbe {
    public static void main(String[] args) {
        SortedMap<Integer, String> base = Collections.synchronizedSortedMap(new TreeMap<>(Map.of(1, "a", 2, "b", 3, "c")));
        SequencedMap<Integer, String> rev = base.reversed();
        int checks = 0, violations = 0;
        for (Map.Entry<Integer, String> e : rev.entrySet()) {
            String old = e.getValue();
            String prev = e.setValue(old + "!");
            checks += 3;
            if (!old.equals(prev)) violations++;
            if (!(old + "!").equals(base.get(e.getKey()))) violations++;
            if (!(old + "!").equals(e.getValue())) violations++;   // Map.Entry: getValue reflects setValue
        }
        Map.Entry<Integer, String> first = rev.firstEntry();
        checks++;
        if (!"c!".equals(first.getValue())) violations++;
        System.out.println("RevEntryProbe checks=" + checks + " violations=" + violations);
    }
}
