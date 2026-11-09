// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/tree/JavaVarKeyword.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.openrewrite.marker.Marker;
import java.util.UUID;
import static org.openrewrite.Tree.randomId;

public class JavaVarKeyword implements Marker {
    private static final JavaVarKeyword INSTANCE = new JavaVarKeyword();
    private final UUID id;

    private JavaVarKeyword() {
        this.id = randomId();
    }

    public static JavaVarKeyword build() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JavaVarKeyword withId(UUID id) {
        throw new UnsupportedOperationException();
    }

    public UUID getId() {
        return this.id;
    }
}
