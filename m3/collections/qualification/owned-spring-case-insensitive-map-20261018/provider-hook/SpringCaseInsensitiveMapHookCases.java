/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0
 * Legal provider test subclasses for the prewritten protocol proposal.
 * Spring6.2.0 donor pin/license and fixture-only scope: FOSS_REUSE_DECISION.tsv.
 */
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.LinkedCaseInsensitiveMap;

/** Baseline binding only; no collection implementation is introduced by this file. */
public final class SpringCaseInsensitiveMapHookCases {
    private SpringCaseInsensitiveMapHookCases() { }
    @FunctionalInterface private interface Action { Object run(); }
    private static final class HookFailure extends RuntimeException {
        @Serial private static final long serialVersionUID = 1L;
    }
    private static class HookMap extends LinkedCaseInsensitiveMap<Object> {
        @Serial private static final long serialVersionUID = 1L;
        private final ArrayList<String> calls = new ArrayList<>();
        private int conversionMode;
        private int eldestMode;
        private int limit = Integer.MAX_VALUE;
        HookMap() { super(Locale.ROOT); }
        @Override protected String convertKey(String key) {
            calls.add("convert:" + key);
            return switch (conversionMode) {
                case 1 -> "fold";
                case 2 -> null;
                case 3 -> throw new HookFailure();
                case 4 -> key == null ? "null-key" : super.convertKey(key);
                default -> super.convertKey(key);
            };
        }
        @Override protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            calls.add("eldest:" + eldest.getKey() + ":" + eldest.getValue() + ":size=" + size());
            if (eldestMode == 1) throw new HookFailure();
            if (eldestMode == 2) calls.add("entrySetterOld:" + eldest.setValue("hook-value"));
            return size() > limit;
        }
    }
    private static final class LocaleOverrideMap extends HookMap {
        @Serial private static final long serialVersionUID = 1L;
        @Override public Locale getLocale() { return Locale.forLanguageTag("tr"); }
    }
    private static String result(Action action) {
        try { return "return=" + action.run(); }
        catch (RuntimeException failure) { return "exception=" + failure.getClass().getSimpleName(); }
    }
    private static String state(Map<String, Object> map) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) entries.add(entry.getKey() + "=" + entry.getValue());
        return "size=" + map.size() + ";entries=" + entries;
    }
    private static void add(List<String> rows, String label, HookMap map, String outcome) {
        rows.add(label + ";" + outcome + ";" + state(map) + ";calls=" + new ArrayList<>(map.calls));
    }
    public static List<String> run() {
        List<String> rows = new ArrayList<>();
        for (int mode : new int[]{0, 1, 2, 4}) {
            HookMap map = new HookMap(); map.conversionMode = mode;
            String output = result(() -> map.put("A", 1)) + ";" + result(() -> map.put("B", 2))
                + ";" + result(() -> map.get("a")) + ";" + result(() -> map.containsKey("b"));
            if (mode == 4) output += ";" + result(() -> map.put(null, 3))
                + ";nullQuery=" + map.get(null) + ";nullContains=" + map.containsKey(null)
                + ";nullKeyViewContains=" + map.keySet().contains(null)
                + ";nullKeyViewRemove=" + map.keySet().remove(null);
            add(rows, "C02/null-collapse-conversion/mode=" + mode, map, output);
        }
        for (String removal : new String[]{"outer", "entry", "iterator", "keyview", "values"}) {
            HookMap map = new HookMap(); map.put("A", 1); map.calls.clear(); map.conversionMode = 3;
            String output = result(() -> switch (removal) {
                case "outer" -> map.remove("a");
                case "entry" -> map.entrySet().remove(new java.util.AbstractMap.SimpleEntry<>("A", 1));
                case "iterator" -> { var cursor = map.entrySet().iterator(); cursor.next(); cursor.remove(); yield null; }
                case "keyview" -> map.keySet().remove("a");
                default -> map.values().remove(1);
            });
            map.conversionMode = 0;
            output += ";lookupAfterFailure=" + map.get("a") + ";containsAfterFailure=" + map.containsKey("a");
            add(rows, "C13-C26-C28/convert-throw-removal/" + removal, map, output);
        }
        HookMap spelling = new HookMap(); spelling.limit = 2;
        spelling.put("A", 1); spelling.put("B", 2); spelling.calls.clear();
        String same = result(() -> spelling.put("A", 3));
        add(rows, "C07-C39/samecase-hook", spelling, same);
        spelling.calls.clear(); String changed = result(() -> spelling.put("a", 4));
        add(rows, "C08-C39/wrongcase-hook", spelling, changed);
        spelling.calls.clear(); String evicted = result(() -> spelling.put("C", 5));
        add(rows, "C39/bounded-eldest", spelling, evicted + ";containsA=" + spelling.containsKey("A"));
        for (int mode : new int[]{1, 2}) {
            HookMap map = new HookMap(); map.eldestMode = mode;
            add(rows, "C39/hook-failure-or-entry-write/mode=" + mode, map, result(() -> map.put("A", 1)));
        }
        HookMap original = new HookMap(); original.limit = 1; original.put("A", 1); original.calls.clear();
        LinkedCaseInsensitiveMap<Object> copy = original.clone();
        String cloneClass = "base=" + (copy.getClass() == LinkedCaseInsensitiveMap.class);
        String copiedPut = result(() -> copy.put("B", 2));
        rows.add("C37-C39/clone-outer-hook;" + cloneClass + ";" + copiedPut + ";clone=" + state(copy)
            + ";original=" + state(original) + ";originalCalls=" + new ArrayList<>(original.calls));
        original.remove("A");
        rows.add("C37/clone-original-removal-view-coupling;cloneContainsA=" + copy.containsKey("A")
            + ";cloneKeyViewContainsA=" + copy.keySet().contains("A") + ";cloneContainsB=" + copy.containsKey("B")
            + ";cloneKeyViewContainsB=" + copy.keySet().contains("B") + ";clone=" + state(copy));
        LinkedCaseInsensitiveMap<Object> secondCopy = copy.clone();
        rows.add("C37/clone-of-clone-view-coupling;containsB=" + secondCopy.containsKey("B")
            + ";keyViewContainsB=" + secondCopy.keySet().contains("B") + ";clone=" + state(secondCopy));
        LocaleOverrideMap locale = new LocaleOverrideMap(); locale.put("I", 1);
        rows.add("C02-C37/subclass-locale-dispatch;locale=" + locale.getLocale().toLanguageTag()
            + ";get_i=" + locale.get("i") + ";get_dotless=" + locale.get("\u0131")
            + ";cloneLocale=" + locale.clone().getLocale().toLanguageTag() + ";state=" + state(locale));
        return rows;
    }
}
