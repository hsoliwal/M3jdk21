@@CLASS
EnumBiMapExample
@@OWNED_CLASS
com.m3.collections.guava.M3EnumBiMap
@@PROVIDER_CLASS
com.google.common.collect.EnumBiMap
@@TYPE
BiMap<Key, Val>
@@IMPORTS
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumBiMap;
import com.m3.collections.guava.M3EnumBiMap;
@@DECLS
    private static final Key[] KEYS = Key.values();
    private static final Val[] VALS = Val.values();

    /** 64-constant key and value domains; an enum-to-enum map holds at most 64 mappings. */
    enum Key { K00, K01, K02, K03, K04, K05, K06, K07, K08, K09, K10, K11, K12, K13, K14, K15, K16, K17, K18, K19, K20, K21, K22, K23, K24, K25, K26, K27, K28, K29, K30, K31, K32, K33, K34, K35, K36, K37, K38, K39, K40, K41, K42, K43, K44, K45, K46, K47, K48, K49, K50, K51, K52, K53, K54, K55, K56, K57, K58, K59, K60, K61, K62, K63 }

    enum Val { V00, V01, V02, V03, V04, V05, V06, V07, V08, V09, V10, V11, V12, V13, V14, V15, V16, V17, V18, V19, V20, V21, V22, V23, V24, V25, V26, V27, V28, V29, V30, V31, V32, V33, V34, V35, V36, V37, V38, V39, V40, V41, V42, V43, V44, V45, V46, V47, V48, V49, V50, V51, V52, V53, V54, V55, V56, V57, V58, V59, V60, V61, V62, V63 }
@@PROVIDER_NEW
EnumBiMap.create(Key.class, Val.class)
@@OWNED_NEW
M3EnumBiMap.create(Key.class, Val.class)
@@DEMO
        m.put(Key.K00, Val.V10);
        m.put(Key.K01, Val.V11);
        LOG.info(who + " get(K00)=" + m.get(Key.K00) + " inverse().get(V11)=" + m.inverse().get(Val.V11) + " keySet=" + m.keySet() + " values=" + m.values());
        m.forcePut(Key.K02, Val.V10);
        LOG.info(who + " after forcePut(K02, V10): " + m + " inverse=" + m.inverse());
        try {
            m.put(Key.K03, Val.V11);
        } catch (IllegalArgumentException e) {
            LOG.info(who + " put(K03, V11) refused: " + e.getMessage());
        }
@@FILL
        int stored = Math.min(n, KEYS.length);
        for (int i = 0; i < stored; i++) { m.put(KEYS[i], VALS[i]); }
        return stored;
@@STEADY
        Key key = KEYS[k & 63];
        m.put(key, VALS[k & 63]);
        m.get(key);
        m.inverse().get(VALS[k & 63]);
