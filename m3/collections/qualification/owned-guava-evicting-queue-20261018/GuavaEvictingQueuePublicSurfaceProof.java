/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.TreeSet;

public final class GuavaEvictingQueuePublicSurfaceProof {
    private GuavaEvictingQueuePublicSurfaceProof() { }
    private static TreeSet<String> surface(Class<?> type,String owner) {
        TreeSet<String> signatures=new TreeSet<>();
        for(Method method:type.getMethods()) {
            String result=method.getReturnType().getTypeName().replace(owner,"DECLARED_OWNER");
            String parameters=Arrays.toString(Arrays.stream(method.getParameterTypes())
                    .map(Class::getTypeName).map(name->name.replace(owner,"DECLARED_OWNER")).toArray());
            signatures.add(method.getName()+parameters+"->"+result+"/static="+Modifier.isStatic(method.getModifiers())+"/varargs="+method.isVarArgs());
        }
        return signatures;
    }
    private static TreeSet<String> constructors(Class<?> type) {
        TreeSet<String> signatures=new TreeSet<>();
        for(Constructor<?> constructor:type.getDeclaredConstructors())
            signatures.add(Modifier.toString(constructor.getModifiers())+Arrays.toString(constructor.getParameterTypes()));
        return signatures;
    }
    public static void main(String[] args) throws Exception {
        Class<?> provider=Class.forName("com.google.common.collect.EvictingQueue");
        Class<?> owned=Class.forName("com.m3.collections.guava.M3EvictingQueue");
        TreeSet<String> left=surface(provider,provider.getName()),right=surface(owned,owned.getName());
        if(!left.equals(right)) {
            TreeSet<String> missing=new TreeSet<>(left);missing.removeAll(right);
            TreeSet<String> extra=new TreeSet<>(right);extra.removeAll(left);
            throw new AssertionError("missing="+missing+"; extra="+extra);
        }
        if(!Modifier.isFinal(provider.getModifiers())||!Modifier.isFinal(owned.getModifiers()))
            throw new AssertionError("final class policy differs");
        if(!constructors(provider).equals(constructors(owned)))
            throw new AssertionError("constructor access/parameters differ");
        if(!java.util.Queue.class.isAssignableFrom(owned)||!java.io.Serializable.class.isAssignableFrom(owned))
            throw new AssertionError("Queue/Serializable contract absent");
        if(java.util.Deque.class.isAssignableFrom(owned))throw new AssertionError("unrequested Deque surface added");
        System.out.println("{\"status\":\"PASS\",\"publicSignaturesIncludingObject\":"+left.size()
                +",\"constructorPolicy\":\"private(int)\",\"classPolicy\":\"final\",\"relocation\":\"declared owner type only\"}");
    }
}
