import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BpProbe : JDK_BACKPORT_PROBE v1 for JDK-8372256 and JDK-8349883 on the patched java.base classes.
 *  chm:    ConcurrentHashMap.equals(map whose get rejects the key type / null) must return false, not throw.
 *  locale: Locale.LanguageRange.parse of ranges ending in '-' must throw IllegalArgumentException (not AIOOBE).
 * Prints one line per check with the observed outcome so stock, live and fixed runs can be compared.
 */
public final class BpProbe {
    public static void main(String[] args) {
        ConcurrentHashMap<Object, Object> chm = new ConcurrentHashMap<>(Map.of("a", 1));
        TreeMap<Object, Object> ints = new TreeMap<>(Map.of(1, 1));
        print("chm.equals(TreeMap<Integer>)", () -> String.valueOf(chm.equals(ints)));
        Map<Object, Object> nullHostile = Map.of("b", 2);
        ConcurrentHashMap<Object, Object> chm2 = new ConcurrentHashMap<>(Map.of("b", 2));
        print("chm.equals(equal Map.of)", () -> String.valueOf(chm2.equals(nullHostile)));
        for (String range : new String[] {"-", "--", "en-", "*-", "en-US"}) {
            print("LanguageRange.parse(\"" + range + "\")", () -> String.valueOf(Locale.LanguageRange.parse(range)));
        }
    }

    interface Check { String run(); }

    static void print(String label, Check c) {
        String out;
        try {
            out = c.run();
        } catch (Throwable t) {
            out = t.getClass().getSimpleName();
        }
        System.out.println(label + " -> " + out);
    }
}
