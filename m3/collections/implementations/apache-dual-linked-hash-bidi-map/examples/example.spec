@@CLASS
DualLinkedHashBidiMapExample
@@OWNED_CLASS
com.m3.collections.apache.M3DualLinkedHashBidiMap
@@PROVIDER_CLASS
org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap
@@TYPE
BidiMap<Integer, Integer>
@@IMPORTS
import com.m3.collections.apache.M3DualLinkedHashBidiMap;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
@@DECLS
@@PROVIDER_NEW
new DualLinkedHashBidiMap<>()
@@OWNED_NEW
new M3DualLinkedHashBidiMap<>()
@@DEMO
        m.put(1, 10);
        m.put(2, 20);
        LOG.info(who + " get(1)=" + m.get(1) + " getKey(20)=" + m.getKey(20) + " inverse=" + m.inverseBidiMap());
        m.put(3, 10);
        LOG.info(who + " after put(3, 10) evicts key 1: " + m + " inverse=" + m.inverseBidiMap());
        StringBuilder walk = new StringBuilder();
        for (MapIterator<Integer, Integer> it = m.mapIterator(); it.hasNext();) {
            it.next();
            walk.append(it.getKey()).append('=').append(it.getValue()).append(' ');
        }
        LOG.info(who + " mapIterator: " + walk.toString().trim());
@@FILL
        for (int i = 0; i < n; i++) { m.put(BOXES[i], BOXES[i + n]); }
        return n;
@@STEADY
        Integer v = m.get(k);
        m.put(k, v);
        m.getKey(v);
