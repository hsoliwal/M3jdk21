// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/style/GeneralFormatStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.style;

public final class GeneralFormatStyle implements Style {
    private final boolean useCRLFNewLines;

    public GeneralFormatStyle(final boolean useCRLFNewLines) {
        this.useCRLFNewLines = useCRLFNewLines;
    }

    public boolean isUseCRLFNewLines() {
        return this.useCRLFNewLines;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof GeneralFormatStyle)) return false;
        final GeneralFormatStyle other = (GeneralFormatStyle) o;
        if (this.isUseCRLFNewLines() != other.isUseCRLFNewLines()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isUseCRLFNewLines() ? 79 : 97);
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "GeneralFormatStyle(useCRLFNewLines=" + this.isUseCRLFNewLines() + ")";
    }
}
