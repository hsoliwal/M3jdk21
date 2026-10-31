@@CLASS
MultiValueMapAdapterExample
@@OWNED_CLASS
com.m3.collections.spring.M3MultiValueMapAdapter
@@PROVIDER_CLASS
org.springframework.util.MultiValueMapAdapter
@@TYPE
MultiValueMap<Integer, Integer>
@@IMPORTS
import com.m3.collections.spring.M3MultiValueMapAdapter;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
@@DECLS
@@PROVIDER_NEW
new MultiValueMapAdapter<>(new LinkedHashMap<>())
@@OWNED_NEW
new M3MultiValueMapAdapter<>(new LinkedHashMap<>())
@@DEMO
        m.add(1, 10);
        m.add(1, 11);
        m.set(2, 20);
        LOG.info(who + " getFirst(1)=" + m.getFirst(1) + " get(1)=" + m.get(1) + " toSingleValueMap=" + m.toSingleValueMap());
        m.addAll(1, List.of(12, 13));
        m.asSingleValueMap().put(3, 30);
        LOG.info(who + " after addAll and single-value put: " + m + " asSingleValueMap=" + m.asSingleValueMap());
@@FILL
        for (int i = 0; i < n; i++) { m.add(BOXES[i & 1023], BOXES[i]); }
        return n;
@@STEADY
        m.add(k, k);
        m.getFirst(k);
        m.get(k).remove(k);
