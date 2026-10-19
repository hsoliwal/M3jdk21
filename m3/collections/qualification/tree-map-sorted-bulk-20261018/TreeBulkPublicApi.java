import java.lang.reflect.*;
import java.util.*;
public final class TreeBulkPublicApi {
    private static String types(Type[] types) {
        return Arrays.toString(Arrays.stream(types).map(Type::getTypeName).toArray(String[]::new));
    }
    public static void main(String[] args) throws Exception {
        Class<?> owner=Class.forName("com.m3.collections.M3TreeMap",false,TreeBulkPublicApi.class.getClassLoader());
        TreeSet<String> rows=new TreeSet<>();
        for(Method m:owner.getMethods()) {
            rows.add("M "+Modifier.toString(m.getModifiers())+" "+m.getName()+" "+m.getGenericReturnType().getTypeName()
                +" "+types(m.getGenericParameterTypes())+" "+types(m.getGenericExceptionTypes())
                +" "+Arrays.toString(m.getTypeParameters())+" bridge="+m.isBridge()+" default="+m.isDefault()
                +" annotations="+Arrays.toString(m.getDeclaredAnnotations()));
        }
        for(Constructor<?> c:owner.getConstructors()) {
            rows.add("C "+Modifier.toString(c.getModifiers())+" "+types(c.getGenericParameterTypes())
                +" "+types(c.getGenericExceptionTypes())+" "+Arrays.toString(c.getTypeParameters())
                +" annotations="+Arrays.toString(c.getDeclaredAnnotations()));
        }
        for(String row:rows)System.out.println(row);
    }
}

