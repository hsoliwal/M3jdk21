@@CLASS
ArrayListMultimapExample
@@OWNED_CLASS
com.m3.collections.guava.M3ArrayListMultimap
@@PROVIDER_CLASS
com.google.common.collect.ArrayListMultimap
@@TYPE
ListMultimap<Integer, Integer>
@@IMPORTS
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.m3.collections.guava.M3ArrayListMultimap;
@@DECLS
@@PROVIDER_NEW
ArrayListMultimap.create()
@@OWNED_NEW
M3ArrayListMultimap.create()
@@DEMO
        m.put(1, 10);
        m.put(1, 11);
        m.put(2, 20);
        LOG.info(who + " get(1)=" + m.get(1) + " containsEntry(2, 20)=" + m.containsEntry(2, 20) + " size=" + m.size());
        m.get(1).add(12);
        m.remove(1, 10);
        LOG.info(who + " after view add and remove: " + m + " keys=" + m.keys() + " asMap=" + m.asMap());
@@FILL
        for (int i = 0; i < n; i++) { m.put(BOXES[i & 1023], BOXES[i]); }
        return n;
@@STEADY
        m.put(k, k);
        m.get(k).size();
        m.remove(k, k);
