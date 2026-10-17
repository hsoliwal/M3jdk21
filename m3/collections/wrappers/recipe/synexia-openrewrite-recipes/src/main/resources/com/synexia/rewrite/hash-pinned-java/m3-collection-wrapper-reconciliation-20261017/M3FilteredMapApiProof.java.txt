// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
import com.m3.collections.M3FilteredMaps;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;

/** Exact inherited overload census, executable against both old and candidate classes. */
public final class M3FilteredMapApiProof {
    private M3FilteredMapApiProof() { }
    public static void main(String[] args) throws Exception {
        boolean expectPresent = args.length == 1 && args[0].equals("present");
        int present = 0;
        for (String name : List.of("unmodifiableMap", "checkedMap", "synchronizedMap")) {
            for (Class<?> shape : List.of(Map.class, SortedMap.class, NavigableMap.class)) {
                Class<?>[] parameters = name.equals("checkedMap")
                        ? new Class<?>[]{shape, Class.class, Class.class} : new Class<?>[]{shape};
                try {
                    Method method = M3FilteredMaps.class.getMethod(name, parameters);
                    if (method.getReturnType() != shape) throw new AssertionError("wrong return shape: " + method);
                    present++;
                } catch (NoSuchMethodException absent) {
                    if (expectPresent) throw new AssertionError(name + "(" + shape.getName() + ") missing", absent);
                }
            }
        }
        if (present != (expectPresent ? 9 : 0)) throw new AssertionError("unexpected overload census: " + present);
        System.out.println("M3_FILTERED_MAP_API_PROOF expected=" + (expectPresent ? "present" : "missing")
                + " present=" + present + " missing=" + (9 - present));
    }
}
