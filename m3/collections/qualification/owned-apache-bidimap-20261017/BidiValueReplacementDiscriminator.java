/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
import java.util.Iterator;
import java.util.function.Supplier;
import com.m3.collections.apache.M3BidiMap;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

public final class BidiValueReplacementDiscriminator {
    private BidiValueReplacementDiscriminator() { }
    private static String run(Supplier<BidiMap<Object,Object>> factory, boolean inverse, String kind) {
        BidiMap<Object,Object> original=factory.get();
        original.put("a",1); original.put("b",2);
        BidiMap<Object,Object> map=inverse ? original.inverseBidiMap() : original;
        Iterator<?> iterator=switch(kind) {
            case "values" -> map.values().iterator();
            case "keys" -> map.keySet().iterator();
            case "entries" -> map.entrySet().iterator();
            case "cursor" -> map.mapIterator();
            default -> throw new AssertionError(kind);
        };
        String first=String.valueOf(iterator.next());
        Object replacement=inverse ? "c" : 3;
        Object old=map.put(inverse ? 1 : "a",replacement);
        iterator.remove();
        return "first="+first+",old="+old+",map="+map+",inverse="+map.inverseBidiMap()+",getKey="+map.getKey(replacement);
    }
    public static void main(String[] args) {
        int cases=0;
        for(boolean inverse:new boolean[]{false,true}) for(String kind:new String[]{"values","keys","entries","cursor"}) {
            String oracle=run(DualLinkedHashBidiMap::new,inverse,kind);
            String candidate=run(M3BidiMap::new,inverse,kind);
            System.out.println("inverse="+inverse+",kind="+kind+",oracle="+oracle+",candidate="+candidate);
            if(!oracle.equals(candidate))throw new AssertionError("NONSTRUCTURAL_VALUES_ITERATOR_REPLACEMENT_DIFFERENCE");
            cases++;
        }
        System.out.println("PASS BIDI_NONSTRUCTURAL_REPLACEMENT cases="+cases);
    }
}
