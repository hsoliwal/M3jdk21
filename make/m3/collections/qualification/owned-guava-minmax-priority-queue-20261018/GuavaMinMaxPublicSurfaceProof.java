/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Queue;

public final class GuavaMinMaxPublicSurfaceProof {
    private GuavaMinMaxPublicSurfaceProof() { }
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
    private static String normalized(Method method,Class<?> owner){return method.toGenericString().replace(owner.getName(),"<OWNER>");}
    public static void main(String[] args) throws Exception{
        Class<?> owner=Class.forName(args[0]);check(Modifier.isPublic(owner.getModifiers())&&Modifier.isFinal(owner.getModifiers()),"public final owner");check(owner.getSuperclass().getName().equals("java.util.AbstractQueue"),"JDK protocol scaffold");check(Queue.class.isAssignableFrom(owner),"Queue");check(!java.util.Deque.class.isAssignableFrom(owner),"not Deque");check(!java.io.Serializable.class.isAssignableFrom(owner),"not Serializable");check(!Cloneable.class.isAssignableFrom(owner),"not Cloneable");check(owner.getConstructors().length==0,"no public constructors");
        List<String> declared=new ArrayList<>(),all=new ArrayList<>(),builders=new ArrayList<>();int staticCount=0,instanceCount=0,bridges=0,objectCount=0;
        for(Method method:owner.getDeclaredMethods())if(Modifier.isPublic(method.getModifiers())){declared.add(normalized(method,owner));if(Modifier.isStatic(method.getModifiers()))staticCount++;else instanceCount++;if(method.isBridge())bridges++;}
        for(Method method:owner.getMethods())if(!Modifier.isStatic(method.getModifiers())){all.add(normalized(method,owner));if(method.getDeclaringClass()==Object.class)objectCount++;}
        check(staticCount==5,"five static factories");check(instanceCount==16,"sixteen declared instance methods");check(bridges==0,"no bridges");check(all.size()==40,"forty instance including Object");check(objectCount==8,"eight Object");
        Class<?> builder=owner.getMethod("orderedBy",Comparator.class).getReturnType();check(Modifier.isPublic(builder.getModifiers())&&Modifier.isFinal(builder.getModifiers())&&Modifier.isStatic(builder.getModifiers()),"public static final Builder");check(builder.getConstructors().length==0,"Builder no public constructor");
        for(Method method:builder.getDeclaredMethods())if(Modifier.isPublic(method.getModifiers()))builders.add(normalized(method,owner));check(builders.size()==4,"four Builder signatures");
        for(Constructor<?> constructor:owner.getConstructors())throw new AssertionError("constructor invention "+constructor);
        declared.sort(String::compareTo);all.sort(String::compareTo);builders.sort(String::compareTo);MessageDigest digest=MessageDigest.getInstance("SHA-256");for(List<String> list:Arrays.asList(declared,all,builders))for(String line:list){System.out.println("PUBLIC_SURFACE\t"+line);digest.update((line+"\n").getBytes(StandardCharsets.UTF_8));}
        System.out.println("{\"status\":\"PASS\",\"factories\":5,\"declaredInstance\":16,\"instanceExcludingObject\":32,\"instanceIncludingObject\":40,\"builderMethods\":4,\"constructors\":0,\"bridges\":0,\"sha256\":\""+HexFormat.of().formatHex(digest.digest())+"\"}");
    }
}
