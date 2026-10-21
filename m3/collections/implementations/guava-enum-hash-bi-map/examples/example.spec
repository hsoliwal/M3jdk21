@@CLASS
EnumHashBiMapExample
@@OWNED_CLASS
com.m3.collections.guava.M3EnumHashBiMap
@@PROVIDER_CLASS
com.google.common.collect.EnumHashBiMap
@@TYPE
BiMap<Key, Integer>
@@IMPORTS
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import com.m3.collections.guava.M3EnumHashBiMap;
@@DECLS
    private static final Key[] KEYS = Key.values();

    /** A 64-constant key domain; an enum-keyed map holds at most this many mappings. */
    enum Key { K00, K01, K02, K03, K04, K05, K06, K07, K08, K09, K10, K11, K12, K13, K14, K15, K16, K17, K18, K19, K20, K21, K22, K23, K24, K25, K26, K27, K28, K29, K30, K31, K32, K33, K34, K35, K36, K37, K38, K39, K40, K41, K42, K43, K44, K45, K46, K47, K48, K49, K50, K51, K52, K53, K54, K55, K56, K57, K58, K59, K60, K61, K62, K63 }
@@PROVIDER_NEW
EnumHashBiMap.create(Key.class)
@@OWNED_NEW
M3EnumHashBiMap.create(Key.class)
@@DEMO
        m.put(Key.K00, 100);
        m.put(Key.K01, 101);
        LOG.info(who + " get(K00)=" + m.get(Key.K00) + " inverse().get(101)=" + m.inverse().get(101) + " containsValue(100)=" + m.containsValue(100));
        m.forcePut(Key.K02, 100);
        LOG.info(who + " after forcePut(K02, 100): " + m + " inverse=" + m.inverse());
        try {
            m.put(Key.K03, 101);
        } catch (IllegalArgumentException e) {
            LOG.info(who + " put(K03, 101) refused: " + e.getMessage());
        }
@@FILL
        int stored = Math.min(n, KEYS.length);
        for (int i = 0; i < stored; i++) { m.put(KEYS[i], BOXES[i]); }
        return stored;
@@STEADY
        Key key = KEYS[k & 63];
        m.put(key, BOXES[k & 63]);
        m.get(key);
        m.inverse().get(BOXES[k & 63]);
