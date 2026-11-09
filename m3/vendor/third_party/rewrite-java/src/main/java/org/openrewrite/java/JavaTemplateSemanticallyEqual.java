// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/JavaTemplateSemanticallyEqual.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
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

import org.antlr.v4.runtime.*;
import org.openrewrite.Cursor;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.TemplateParameterLexer;
import org.openrewrite.java.internal.grammar.TemplateParameterParser;
import org.openrewrite.java.internal.grammar.TemplateParameterParser.TypedPatternContext;
import org.openrewrite.java.internal.template.TemplateParameter;
import org.openrewrite.java.internal.template.TypeParameter;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import java.util.*;
import static org.openrewrite.Tree.randomId;

class JavaTemplateSemanticallyEqual extends SemanticallyEqual {

    static final class TemplateMatchResult {
        private final boolean match;
        private final List<J> matchedParameters;

        public TemplateMatchResult(final boolean match, final List<J> matchedParameters) {
            this.match = match;
            this.matchedParameters = matchedParameters;
        }

        public boolean isMatch() {
            return this.match;
        }

        public List<J> getMatchedParameters() {
            return this.matchedParameters;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof JavaTemplateSemanticallyEqual.TemplateMatchResult)) return false;
            final JavaTemplateSemanticallyEqual.TemplateMatchResult other = (JavaTemplateSemanticallyEqual.TemplateMatchResult) o;
            if (this.isMatch() != other.isMatch()) return false;
            final Object this$matchedParameters = this.getMatchedParameters();
            final Object other$matchedParameters = other.getMatchedParameters();
            if (this$matchedParameters == null ? other$matchedParameters != null : !this$matchedParameters.equals(other$matchedParameters)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + (this.isMatch() ? 79 : 97);
            final Object $matchedParameters = this.getMatchedParameters();
            result = result * PRIME + ($matchedParameters == null ? 43 : $matchedParameters.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "JavaTemplateSemanticallyEqual.TemplateMatchResult(match=" + this.isMatch() + ", matchedParameters=" + this.getMatchedParameters() + ")";
        }
    }

    static TemplateMatchResult matchesTemplate(JavaTemplate template, Cursor input) {
        JavaCoordinates coordinates;
        if (input.getValue() instanceof Expression) {
            coordinates = ((Expression) input.getValue()).getCoordinates().replace();
        } else if (input.getValue() instanceof Statement) {
            coordinates = ((Statement) input.getValue()).getCoordinates().replace();
        } else {
            throw new IllegalArgumentException("Only expressions and statements can be matched against a template: " + input.getClass());
        }
        J[] parameters = createTemplateParameters(template.getCode());
        try {
            J templateTree = template.apply(input, coordinates, (Object[]) parameters);
            return matchTemplate(templateTree, input);
        } catch (RuntimeException e) {
            // FIXME this is just a workaround, as template matching finds many new corner cases in `JavaTemplate` which we need to fix
            return new TemplateMatchResult(false, Collections.emptyList());
        }
    }

    private static J[] createTemplateParameters(String code) {
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("#{", "}", null);
        List<J> parameters = new ArrayList<>();
        String substituted = code;
        Map<String, String> typedPatternByName = new HashMap<>();
        while (true) {
            String previous = substituted;
            substituted = propertyPlaceholderHelper.replacePlaceholders(substituted, key -> {
                String s;
                if (!key.isEmpty()) {
                    TemplateParameterParser parser = new TemplateParameterParser(new CommonTokenStream(new TemplateParameterLexer(CharStreams.fromString(key))));
                    parser.removeErrorListeners();
                    parser.addErrorListener(new BaseErrorListener() {
                        @Override
                        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                            throw new IllegalArgumentException(String.format("Syntax error at line %d:%d %s.", line, charPositionInLine, msg), e);
                        }
                    });
                    TemplateParameterParser.MatcherPatternContext ctx = parser.matcherPattern();
                    if (ctx.typedPattern() == null) {
                        String paramName = ctx.parameterName().Identifier().getText();
                        s = typedPatternByName.get(paramName);
                        if (s == null) {
                            throw new IllegalArgumentException("The parameter " + paramName + " must be defined before it is referenced.");
                        }
                    } else {
                        TypedPatternContext typedPattern = ctx.typedPattern();
                        JavaType type = typedParameter(key, typedPattern);
                        s = TypeUtils.toString(type);
                        String name = null;
                        if (typedPattern.parameterName() != null) {
                            name = typedPattern.parameterName().Identifier().getText();
                            typedPatternByName.put(name, s);
                        }
                        Markers markers = Markers.build(Collections.singleton(new TemplateParameter(randomId(), type, name)));
                        parameters.add(new J.Empty(randomId(), Space.EMPTY, markers));
                    }
                } else {
                    throw new IllegalArgumentException("Only typed placeholders are allowed.");
                }
                return s;
            });
            if (previous.equals(substituted)) {
                break;
            }
        }
        return parameters.toArray(new J[0]);
    }

    private static JavaType typedParameter(String key, TypedPatternContext typedPattern) {
        String matcherName = typedPattern.patternType().matcherName().Identifier().getText();
        if ("any".equals(matcherName)) {
            return TypeParameter.toFullyQualifiedName(typedPattern.patternType().type());
        } else if ("anyArray".equals(matcherName)) {
            return new JavaType.Array(null, TypeParameter.toFullyQualifiedName(typedPattern.patternType().type()), null);
        } else {
            throw new IllegalArgumentException("Unsupported template matcher \'" + key + "\'");
        }
    }

    private static TemplateMatchResult matchTemplate(J templateTree, Cursor cursor) {
        if (templateTree == cursor.getValue()) {
            // When `JavaTemplate#apply()` returns the input itself, it could not be matched
            return new TemplateMatchResult(false, Collections.emptyList());
        }
        JavaTemplateSemanticallyEqualVisitor semanticallyEqualVisitor = new JavaTemplateSemanticallyEqualVisitor();
        semanticallyEqualVisitor.visit(templateTree, cursor.getValue(), cursor.getParentOrThrow());
        return new TemplateMatchResult(semanticallyEqualVisitor.isEqual(), new ArrayList<>(semanticallyEqualVisitor.matchedParameters.keySet()));
    }


    @SuppressWarnings("ConstantConditions")
    private static class JavaTemplateSemanticallyEqualVisitor extends SemanticallyEqualVisitor {
        final Map<J, String> matchedParameters = new LinkedHashMap<>();

        public JavaTemplateSemanticallyEqualVisitor() {
            super(true);
        }

        private boolean matchTemplateParameterPlaceholder(J.Empty empty, J j) {
            if (j instanceof TypedTree) {
                if (j instanceof J.Primitive || j instanceof J.Identifier && ((J.Identifier) j).getFieldType() == null) {
                    // don't match types, only expressions
                    return false;
                }
                TemplateParameter marker = (TemplateParameter) empty.getMarkers().getMarkers().get(0);
                if (marker.getName() != null) {
                    for (Map.Entry<J, String> matchedParameter : matchedParameters.entrySet()) {
                        if (matchedParameter.getValue().equals(marker.getName())) {
                            if (!SemanticallyEqual.areEqual(matchedParameter.getKey(), j)) {
                                return false;
                            }
                        }
                    }
                }
                if (TypeUtils.isObject(marker.getType()) || TypeUtils.isAssignableTo(marker.getType(), ((TypedTree) j).getType())) {
                    registerMatch(j, marker.getName());
                    return true;
                }
            }
            return false;
        }

        private void registerMatch(J j, @Nullable String name) {
            matchedParameters.put(j, name);
        }

        @Override
        public J.Empty visitEmpty(J.Empty empty, J j) {
            if (isEqual.get()) {
                if (isTemplateParameterPlaceholder(empty)) {
                    isEqual.set(matchTemplateParameterPlaceholder(empty, j));
                    return empty;
                }
                if (!(j instanceof J.Empty)) {
                    isEqual.set(false);
                    return empty;
                }
                J.Empty compareTo = (J.Empty) j;
                if (nullMissMatch(empty.getType(), compareTo.getType())) {
                    isEqual.set(false);
                    return empty;
                }
            }
            return empty;
        }

        private static boolean isTemplateParameterPlaceholder(J.Empty empty) {
            Markers markers = empty.getMarkers();
            return markers.getMarkers().size() == 1 && markers.getMarkers().get(0) instanceof TemplateParameter;
        }
    }
}
