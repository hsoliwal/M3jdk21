// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/TypeMatcher.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.MethodSignatureLexer;
import org.openrewrite.java.internal.grammar.MethodSignatureParser;
import org.openrewrite.java.internal.grammar.MethodSignatureParserBaseVisitor;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import java.util.regex.Pattern;
import static org.openrewrite.java.tree.TypeUtils.fullyQualifiedNamesAreEqual;

@SuppressWarnings("NotNullFieldNotInitialized")
public class TypeMatcher {
    private static final String ASPECTJ_DOT_PATTERN = StringUtils.aspectjNameToPattern(".");
    @SuppressWarnings("NotNullFieldNotInitialized")
    private Pattern targetTypePattern;
    @Nullable
    private String targetType;
    private final String signature;
    /**
     * Whether to match on subclasses of {@link #targetTypePattern}.
     */
    private final boolean matchInherited;

    public boolean matches(@Nullable TypeTree tt) {
        return tt != null && matches(tt.getType());
    }

    public boolean matchesPackage(String packageName) {
        return targetTypePattern.matcher(packageName).matches() || targetTypePattern.matcher(packageName.replaceAll("\\.\\*$", "." + signature.substring(signature.lastIndexOf('.') + 1))).matches();
    }

    public TypeMatcher(@Nullable String fieldType) {
        this(fieldType, false);
    }

    public TypeMatcher(@Nullable String fieldType, boolean matchInherited) {
        this.signature = fieldType == null ? ".*" : fieldType;
        this.matchInherited = matchInherited;
        if (StringUtils.isBlank(fieldType)) {
            targetTypePattern = Pattern.compile(".*");
        } else {
            MethodSignatureParser parser = new MethodSignatureParser(new CommonTokenStream(new MethodSignatureLexer(CharStreams.fromString(fieldType))));
            new MethodSignatureParserBaseVisitor<Void>() {
                @Override
                public Void visitTargetTypePattern(MethodSignatureParser.TargetTypePatternContext ctx) {
                    String pattern = new TypeVisitor().visitTargetTypePattern(ctx);
                    targetTypePattern = Pattern.compile(new TypeVisitor().visitTargetTypePattern(ctx));
                    targetType = isPlainIdentifier(ctx) ? pattern.replace(ASPECTJ_DOT_PATTERN, ".").replace("\\", "") : null;
                    return null;
                }
            }.visitTargetTypePattern(parser.targetTypePattern());
        }
    }

    public boolean matches(@Nullable JavaType type) {
        return TypeUtils.isOfTypeWithName(TypeUtils.asFullyQualified(type), matchInherited, this::matchesTargetTypeName);
    }

    private boolean matchesTargetTypeName(String fullyQualifiedTypeName) {
        return this.targetType != null && fullyQualifiedNamesAreEqual(this.targetType, fullyQualifiedTypeName) || this.targetType == null && this.targetTypePattern.matcher(fullyQualifiedTypeName).matches();
    }

    private static boolean isPlainIdentifier(MethodSignatureParser.TargetTypePatternContext context) {
        return context.BANG() == null && context.AND() == null && context.OR() == null && context.classNameOrInterface().DOTDOT().isEmpty() && context.classNameOrInterface().WILDCARD().isEmpty();
    }

    @Nullable
    public String getTargetType() {
        return this.targetType;
    }

    public String getSignature() {
        return this.signature;
    }

    public Pattern getTargetTypePattern() {
        return this.targetTypePattern;
    }

    /**
     * Whether to match on subclasses of {@link #targetTypePattern}.
     */
    public boolean isMatchInherited() {
        return this.matchInherited;
    }
}
