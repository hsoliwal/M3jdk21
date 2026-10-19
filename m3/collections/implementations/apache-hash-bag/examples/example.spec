@@CLASS
HashBagExample
@@OWNED_CLASS
com.m3.collections.apache.M3HashBag
@@PROVIDER_CLASS
org.apache.commons.collections4.bag.HashBag
@@TYPE
Bag<Integer>
@@IMPORTS
import com.m3.collections.apache.M3HashBag;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
@@DECLS
@@PROVIDER_NEW
new HashBag<>()
@@OWNED_NEW
new M3HashBag<>()
@@DEMO
        m.add(1);
        m.add(1);
        m.add(2, 3);
        LOG.info(who + " getCount(1)=" + m.getCount(1) + " getCount(2)=" + m.getCount(2) + " size=" + m.size() + " uniqueSet=" + m.uniqueSet());
        m.remove(2, 2);
        m.remove(1);
        LOG.info(who + " after remove(2, 2) and remove(1): " + m + " size=" + m.size());
@@FILL
        for (int i = 0; i < n; i++) { m.add(BOXES[i & 8191]); }
        return n;
@@STEADY
        m.add(k);
        m.getCount(k);
        m.remove(k, 1);
