// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** Conservative source contract projection: non-private declarations, all fields, imports and initializers. */
public final class JavaContractSurface {
    private JavaContractSurface() { }
    public static String canonical(String source) {
        JavaSourceUnit parsed=JavaSourceUnit.parse(source);
        StringBuilder out=new StringBuilder("JAVA-SEALED-SOURCE-SURFACE/1\n");
        add(out, String.valueOf(parsed.unit.getPackage()), parsed.unit.getImports().toString(),
                String.valueOf(parsed.unit.getModule()));
        for (Tree declaration:parsed.unit.getTypeDecls()) {
            if (declaration instanceof ClassTree type) type(out,type);
            else add(out,declaration.toString());
        }
        return out.toString();
    }
    public static String root(SealedSources sources, Set<String> paths) {
        StringBuilder material=new StringBuilder("JAVA-SURFACE-SET/1\n");
        paths.stream().sorted().forEach(path -> {
            if (!path.endsWith(".java")) throw new IllegalArgumentException("JAVA_SURFACE_PATH");
            material.append(SealHash.frame(path,canonical(sources.require(path)))).append('\n');
        });
        return SealHash.text(material.toString());
    }
    public static boolean equivalent(String before, String candidate) { return canonical(before).equals(canonical(candidate)); }
    private static void type(StringBuilder out,ClassTree type) {
        add(out,"TYPE",type.getKind().name(),type.getSimpleName().toString(),type.getModifiers().toString(),
                type.getTypeParameters().toString(),String.valueOf(type.getExtendsClause()),
                type.getImplementsClause().toString(),type.getPermitsClause().toString());
        for (Tree member:type.getMembers()) {
            if (member instanceof ClassTree nested) type(out,nested);
            else if (member instanceof MethodTree method) {
                if (!method.getModifiers().getFlags().contains(Modifier.PRIVATE)) add(out,"METHOD",
                        method.getName().toString(),method.getModifiers().toString(),method.getTypeParameters().toString(),
                        String.valueOf(method.getReturnType()),method.getParameters().toString(),
                        String.valueOf(method.getReceiverParameter()),method.getThrows().toString(),String.valueOf(method.getDefaultValue()));
            } else add(out,"MEMBER",member.getKind().name(),member.toString());
        }
        add(out,"END_TYPE");
    }
    private static void add(StringBuilder out,String... values) { out.append(SealHash.frame(values)).append('\n'); }
}
