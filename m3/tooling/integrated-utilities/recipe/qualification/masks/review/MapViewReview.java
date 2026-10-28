import java.util.*;
public class MapViewReview {
    static void show(String name, Runnable action) {
        try { action.run(); } catch (Throwable problem) { System.out.println(name + " -> " + problem); }
    }
    public static void main(String[] args) {
        NavigableSet<Integer> bounds = new TreeSet<>(List.of(1,2,3,4,5)).subSet(2,true,5,false);
        NavigableSet<Integer> set = Collections.filteredNavigableSet(bounds, x -> true);
        show("bounded contains(-1)", () -> System.out.println("bounded contains(-1): " + set.contains(-1)));
        show("bounded lower(99)", () -> System.out.println("bounded lower(99): " + set.lower(99)));
        show("bounded ceiling(-1)", () -> System.out.println("bounded ceiling(-1): " + set.ceiling(-1)));
        Object key = new String("same"), first = new String("value"), second = new String("value");
        IdentityHashMap<Object,Object> original = new IdentityHashMap<>(); original.put(key, first);
        IdentityHashMap<Object,Object> other = new IdentityHashMap<>(); other.put(key, second);
        Map<Object,Object> filtered = Collections.filteredMap(original, (k,v) -> true);
        System.out.println("identity map equals: " + filtered.equals(other) + " reverse=" + other.equals(filtered));
        System.out.println("identity keySet equals: " + filtered.keySet().equals(original.keySet()) + " reverse=" + original.keySet().equals(filtered.keySet())
                + " hash=" + filtered.keySet().hashCode() + " backingHash=" + original.keySet().hashCode());
        LinkedHashMap<String,Integer> access = new LinkedHashMap<>(16, .75f, true);
        access.put("a",2); access.put("b",4);
        Map<String,Integer> accessView = Collections.filteredMap(access, (k,v) -> true);
        accessView.get("a");
        System.out.println("access-order after filtered get(a): " + access.keySet());
        TreeSet<String> spellings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); spellings.add("hidden");
        SortedSet<String> upper = Collections.filteredSortedSet(spellings, s -> Character.isUpperCase(s.charAt(0)));
        System.out.println("hidden collision add(HIDDEN)=" + upper.add("HIDDEN") + " then contains(HIDDEN)=" + upper.contains("HIDDEN"));
    }
}
