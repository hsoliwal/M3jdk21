// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/internal/template/JavaTemplateLanguageExtension.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.internal.template;

import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.java.tree.Space;

/**
 * Each language that extends {@link J} should provide a template extension that helps
 * {@link JavaTemplate} build the telescoping name scope around the coordinate. When that process
 * encounters a language-specific grammatical construct, it is this extension that will help
 * build it out.
 */
public abstract class JavaTemplateLanguageExtension {
    protected final JavaTemplateParser templateParser;
    protected final Substitutions substitutions;
    protected final String substitutedTemplate;
    protected final JavaCoordinates coordinates;
    protected final Tree insertionPoint;
    protected final Space.Location loc;
    protected final JavaCoordinates.Mode mode;

    public JavaTemplateLanguageExtension(JavaTemplateParser templateParser, Substitutions substitutions, String substitutedTemplate, JavaCoordinates coordinates) {
        this.templateParser = templateParser;
        this.substitutions = substitutions;
        this.substitutedTemplate = substitutedTemplate;
        this.coordinates = coordinates;
        this.insertionPoint = coordinates.getTree();
        this.loc = coordinates.getSpaceLocation();
        this.mode = coordinates.getMode();
    }

    public abstract TreeVisitor<? extends J, Integer> getMixin();
}
