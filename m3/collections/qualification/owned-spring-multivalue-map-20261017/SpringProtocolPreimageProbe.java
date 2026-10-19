import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import com.m3.collections.M3LinkedHashMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

public final class SpringProtocolPreimageProbe {
    private SpringProtocolPreimageProbe() { }
    private static void singleton(String label) {
        List<String> calls=new ArrayList<>();
        Predicate<String> predicate=label.equals("null") ? null : value->{calls.add(value); return label.equals("true");};
        try { System.out.println("removeIf="+label+",result="+Collections.singletonList("x").removeIf(predicate)+",calls="+calls); }
        catch(RuntimeException failure) { System.out.println("removeIf="+label+",error="+failure.getClass().getSimpleName()+",calls="+calls); }
    }
    private static void callback(String label, MultiValueMap<String,Integer> map) {
        map.add("a",1);map.add("b",2);List<String> calls=new ArrayList<>();
        try {map.forEach((key,value)->{calls.add(key); if(key.equals("a"))map.add("c",3);}); System.out.println(label+",completed,calls="+calls+",state="+map);}
        catch(RuntimeException failure) {System.out.println(label+",error="+failure.getClass().getSimpleName()+",calls="+calls+",state="+map);}
    }
    public static void main(String[] args) {
        singleton("false");singleton("true");singleton("null");
        callback("concrete-provider",new LinkedMultiValueMap<>());
        callback("existing-M3-owner-protocol",new MultiValueMapAdapter<>(new M3LinkedHashMap<>()));
    }
}
