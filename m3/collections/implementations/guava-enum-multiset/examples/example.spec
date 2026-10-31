@@CLASS
EnumMultisetExample
@@OWNED_CLASS
com.m3.collections.guava.M3EnumMultiset
@@PROVIDER_CLASS
com.google.common.collect.EnumMultiset
@@TYPE
Multiset<Key>
@@IMPORTS
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import com.m3.collections.guava.M3EnumMultiset;
@@DECLS
    private static final Key[] KEYS = Key.values();

    /** A 64-constant element domain; counts are per constant. */
    enum Key { K00, K01, K02, K03, K04, K05, K06, K07, K08, K09, K10, K11, K12, K13, K14, K15, K16, K17, K18, K19, K20, K21, K22, K23, K24, K25, K26, K27, K28, K29, K30, K31, K32, K33, K34, K35, K36, K37, K38, K39, K40, K41, K42, K43, K44, K45, K46, K47, K48, K49, K50, K51, K52, K53, K54, K55, K56, K57, K58, K59, K60, K61, K62, K63 }
@@PROVIDER_NEW
EnumMultiset.create(Key.class)
@@OWNED_NEW
M3EnumMultiset.create(Key.class)
@@DEMO
        m.add(Key.K00);
        m.add(Key.K00);
        m.add(Key.K01, 3);
        LOG.info(who + " count(K00)=" + m.count(Key.K00) + " count(K01)=" + m.count(Key.K01) + " size=" + m.size() + " entrySet=" + m.entrySet());
        m.setCount(Key.K01, 1);
        m.remove(Key.K00);
        LOG.info(who + " after setCount and remove: " + m + " elementSet=" + m.elementSet());
@@FILL
        for (int i = 0; i < n; i++) { m.add(KEYS[i & 63]); }
        return n;
@@STEADY
        Key key = KEYS[k & 63];
        m.add(key);
        m.count(key);
        m.remove(key);
