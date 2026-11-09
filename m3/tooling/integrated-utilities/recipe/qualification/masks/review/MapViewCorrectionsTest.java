import java.util.*;
public class MapViewCorrectionsTest {
    private static int cases;
    static void check(boolean condition) { cases++; if (!condition) throw new AssertionError("case " + cases); }
    static void rejected(Runnable action) {
        try { action.run(); throw new AssertionError("hidden collision accepted"); }
        catch (IllegalArgumentException expected) { cases++; }
    }
    static Map<Object,Object> wrap(Map<Object,Object> source, int mode) {
        return switch (mode) {
            case 0 -> source;
            case 1 -> Collections.unmodifiableMap(source);
            case 2 -> Collections.synchronizedMap(source);
            case 3 -> Collections.checkedMap(source, Object.class, Object.class);
            default -> throw new AssertionError();
        };
    }
    record Flag(int id, boolean visible) {
        @Override public boolean equals(Object other) { return other instanceof Flag f && id == f.id; }
        @Override public int hashCode() { return id; }
    }
    public static void main(String[] args) {
        NavigableSet<Integer> base = new TreeSet<>(List.of(1,2,3,4,5)).subSet(2,true,5,false);
        NavigableSet<Integer> view = Collections.filteredNavigableSet(base, value -> true);
        check(!view.contains(-1)); check(!view.remove(99)); check(view.lower(99) == 4); check(view.ceiling(-1) == 2);
        NavigableMap<Integer,Integer> tree = new TreeMap<>(); for (int i=1;i<=5;i++) tree.put(i,i);
        NavigableMap<Integer,Integer> map = Collections.filteredNavigableMap(tree.subMap(2,true,5,false), (k,v) -> true);
        check(map.lowerKey(99) == 4); check(map.ceilingKey(-1) == 2);
        for (int mode=0;mode<4;mode++) {
            Object key = new String("same"), value = new String("value");
            Map<Object,Object> identity = new IdentityHashMap<>(); identity.put(key,value);
            Map<Object,Object> filtered = Collections.filteredMap(wrap(identity,mode), (k,v) -> true);
            Map<Object,Object> same = new IdentityHashMap<>(); same.put(key,value);
            Map<Object,Object> different = new IdentityHashMap<>(); different.put(key,new String("value"));
            check(filtered.equals(same)); check(same.equals(filtered)); check(filtered.hashCode() == same.hashCode());
            check(!filtered.equals(different)); check(!different.equals(filtered));
            check(filtered.keySet().equals(identity.keySet())); check(identity.keySet().equals(filtered.keySet()));
            check(filtered.keySet().hashCode() == identity.keySet().hashCode());
            Map<Object,Object> access = new LinkedHashMap<>(16,.75f,true); access.put("a",2); access.put("b",4);
            Map<Object,Object> accesses = Collections.filteredMap(wrap(access,mode), (k,v) -> true);
            accesses.get("a"); check(new ArrayList<>(access.keySet()).equals(List.of("b","a")));
            accesses.getOrDefault("b",99); check(new ArrayList<>(access.keySet()).equals(List.of("a","b")));
        }
        Map<Object,Object> normal = new HashMap<>(); normal.put("k",new String("value"));
        Map<Object,Object> normalFiltered = Collections.filteredMap(normal, (k,v) -> true);
        check(normalFiltered.equals(Map.of("k",new String("value"))));
        TreeSet<String> spellings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); spellings.add("hidden");
        SortedSet<String> upper = Collections.filteredSortedSet(spellings, s -> Character.isUpperCase(s.charAt(0)));
        rejected(() -> upper.add("HIDDEN")); rejected(() -> upper.addAll(List.of("VISIBLE","HIDDEN")));
        check(spellings.equals(new TreeSet<>(List.of("hidden"))));
        Set<Flag> flags = new HashSet<>(); flags.add(new Flag(1,false));
        Set<Flag> visible = Collections.filteredSet(flags, Flag::visible);
        rejected(() -> visible.add(new Flag(1,true)));
        rejected(() -> visible.addAll(List.of(new Flag(2,true),new Flag(1,true))));
        check(flags.size() == 1 && visible.isEmpty());
        List<Integer> source = new ArrayList<>(List.of(1,2,3,4,5));
        List<Integer> selected = Collections.filteredList(source, value -> (value & 1) == 0);
        selected.reversed().add(6);
        check(source.equals(List.of(1,6,2,3,4,5))); check(selected.reversed().reversed() == selected);
        System.out.println("MapViewCorrectionsTest PASS cases=" + cases);
    }
}
