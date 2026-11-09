@@CLASS
HashMultisetExample
@@OWNED_CLASS
com.m3.collections.guava.M3HashMultiset
@@PROVIDER_CLASS
com.google.common.collect.HashMultiset
@@TYPE
Multiset<Integer>
@@IMPORTS
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.m3.collections.guava.M3HashMultiset;
@@DECLS
@@PROVIDER_NEW
HashMultiset.create()
@@OWNED_NEW
M3HashMultiset.create()
@@DEMO
        m.add(1);
        m.add(1);
        m.add(2, 3);
        LOG.info(who + " count(1)=" + m.count(1) + " count(2)=" + m.count(2) + " size=" + m.size() + " elementSet=" + m.elementSet());
        m.setCount(2, 1);
        m.remove(1);
        LOG.info(who + " after setCount and remove: entrySet=" + m.entrySet() + " size=" + m.size());
@@FILL
        for (int i = 0; i < n; i++) { m.add(BOXES[i & 8191]); }
        return n;
@@STEADY
        m.add(k);
        m.count(k);
        m.remove(k);
