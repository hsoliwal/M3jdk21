/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0
 * Adapted firstparty public-surface reflection pattern. Source-only until owner exists.
 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/** Exact public/generic/default/bridge/hook frame; storage audit is separate. */
public final class SpringCaseInsensitiveMapFullSurfaceProof {
    private SpringCaseInsensitiveMapFullSurfaceProof() { }
    private static String normalized(String text, Class<?> self) {
        return text.replace(self.getName(), "DECLARED_OWNER");
    }
    private static String types(Type[] types, Class<?> self) {
        return Arrays.toString(Arrays.stream(types).map(Type::getTypeName)
            .map(name -> normalized(name, self)).toArray());
    }
    private static String variables(TypeVariable<?>[] variables, Class<?> self) {
        return Arrays.toString(Arrays.stream(variables).map(variable -> variable.getName()
            + ":" + types(variable.getBounds(), self)).toArray());
    }
    private static String method(Method method, Class<?> self) {
        return method.getName() + types(method.getGenericParameterTypes(), self) + "->"
            + normalized(method.getGenericReturnType().getTypeName(), self)
            + "/rawParameters=" + types(method.getParameterTypes(), self)
            + "/rawReturn=" + normalized(method.getReturnType().getTypeName(), self)
            + "/declaring=" + normalized(method.getDeclaringClass().getName(), self)
            + "/modifiers=" + Modifier.toString(method.getModifiers())
            + "/typeVariables=" + variables(method.getTypeParameters(), self)
            + "/throws=" + types(method.getGenericExceptionTypes(), self)
            + "/bridge=" + method.isBridge() + "/synthetic=" + method.isSynthetic()
            + "/default=" + method.isDefault() + "/varargs=" + method.isVarArgs();
    }
    private static TreeSet<String> publicMethods(Class<?> self) {
        TreeSet<String> result = new TreeSet<>();
        for (Method method : self.getMethods()) result.add(method(method, self));
        return result;
    }
    private static TreeSet<String> publicConstructors(Class<?> self) {
        TreeSet<String> result = new TreeSet<>();
        for (Constructor<?> constructor : self.getConstructors()) {
            result.add(Modifier.toString(constructor.getModifiers()) + types(constructor.getGenericParameterTypes(), self)
                + "/typeVariables=" + variables(constructor.getTypeParameters(), self)
                + "/throws=" + types(constructor.getGenericExceptionTypes(), self)
                + "/varargs=" + constructor.isVarArgs());
        }
        return result;
    }
    private static TreeSet<String> declaredProtected(Class<?> self) {
        TreeSet<String> result = new TreeSet<>();
        for (Method method : self.getDeclaredMethods()) if (Modifier.isProtected(method.getModifiers())) result.add(method(method, self));
        return result;
    }
    private static String frame(Class<?> self) {
        return Modifier.toString(self.getModifiers()) + "/parent=" + self.getSuperclass().getName()
            + "/interfaces=" + types(self.getGenericInterfaces(), self)
            + "/typeVariables=" + variables(self.getTypeParameters(), self);
    }
    private static void same(TreeSet<String> expected, TreeSet<String> actual, String label) {
        if (!expected.equals(actual)) {
            TreeSet<String> missing = new TreeSet<>(expected); missing.removeAll(actual);
            TreeSet<String> extra = new TreeSet<>(actual); extra.removeAll(expected);
            throw new AssertionError(label + " missing=" + missing + "; extra=" + extra);
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("exact provider and actual root-assigned owned class required");
        Class<?> provider = Class.forName(args[0]), owned = Class.forName(args[1]);
        if (!provider.getName().equals("org.springframework.util.LinkedCaseInsensitiveMap")) throw new AssertionError("wrong provider");
        Method[] methods = provider.getMethods();
        long bridges = Arrays.stream(methods).filter(Method::isBridge).count();
        long semantic = Arrays.stream(methods).filter(method -> !method.isBridge() && !Modifier.isStatic(method.getModifiers())).count();
        List<Method> defaults = Arrays.stream(methods).filter(Method::isDefault).toList();
        if (provider.getConstructors().length != 4 || semantic != 34 || bridges != 4 || defaults.size() != 7
                || defaults.stream().anyMatch(method -> method.getDeclaringClass() != java.util.Map.class)) {
            throw new AssertionError("provider4/34/4/7 frame drift");
        }
        if (!frame(provider).equals(frame(owned))) throw new AssertionError("class/interface/typeparameter frame differs");
        same(publicConstructors(provider), publicConstructors(owned), "public constructors");
        same(publicMethods(provider), publicMethods(owned), "public generic/default/bridge methods");
        same(declaredProtected(provider), declaredProtected(owned), "protected hooks");
        if (declaredProtected(provider).size() != 2) throw new AssertionError("protectedhook frame drift");
        System.out.println("{\"status\":\"PASS\",\"publicConstructors\":4,\"nonbridgeInstanceMethods\":34,"
            + "\"bridges\":4,\"mapDefaults\":7,\"protectedHooks\":2,\"normalization\":\"exact owner type names only\","
            + "\"scope\":\"public metadata only; behavior and provider-free array ownership require separate proofs\"}");
    }
}
