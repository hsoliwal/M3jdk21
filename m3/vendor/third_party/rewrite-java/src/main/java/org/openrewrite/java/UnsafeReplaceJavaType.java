// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/UnsafeReplaceJavaType.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Incubating(since = "7.17.0")
final class UnsafeReplaceJavaType extends UnsafeJavaTypeVisitor<Integer> {
    private final JavaType replace;
    private final JavaType replaceWith;
    private final Set<JavaType> stack = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public JavaType visit(@Nullable JavaType javaType, Integer p) {
        if (javaType == null) {
            //noinspection ConstantConditions
            return null;
        }
        if (javaType == replace) {
            return replaceWith;
        }
        if (stack.add(javaType)) {
            return super.visit(javaType, p);
        }
        return javaType;
    }

    public UnsafeReplaceJavaType(final JavaType replace, final JavaType replaceWith) {
        this.replace = replace;
        this.replaceWith = replaceWith;
    }

    public JavaType getReplace() {
        return this.replace;
    }

    public JavaType getReplaceWith() {
        return this.replaceWith;
    }

    public Set<JavaType> getStack() {
        return this.stack;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "UnsafeReplaceJavaType(replace=" + this.getReplace() + ", replaceWith=" + this.getReplaceWith() + ", stack=" + this.getStack() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof UnsafeReplaceJavaType)) return false;
        final UnsafeReplaceJavaType other = (UnsafeReplaceJavaType) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$replace = this.getReplace();
        final Object other$replace = other.getReplace();
        if (this$replace == null ? other$replace != null : !this$replace.equals(other$replace)) return false;
        final Object this$replaceWith = this.getReplaceWith();
        final Object other$replaceWith = other.getReplaceWith();
        if (this$replaceWith == null ? other$replaceWith != null : !this$replaceWith.equals(other$replaceWith)) return false;
        final Object this$stack = this.getStack();
        final Object other$stack = other.getStack();
        if (this$stack == null ? other$stack != null : !this$stack.equals(other$stack)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof UnsafeReplaceJavaType;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $replace = this.getReplace();
        result = result * PRIME + ($replace == null ? 43 : $replace.hashCode());
        final Object $replaceWith = this.getReplaceWith();
        result = result * PRIME + ($replaceWith == null ? 43 : $replaceWith.hashCode());
        final Object $stack = this.getStack();
        result = result * PRIME + ($stack == null ? 43 : $stack.hashCode());
        return result;
    }
}
