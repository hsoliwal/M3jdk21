// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/Maintainer.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import java.net.URI;

public final class Maintainer {
    /**
     * The name of the organization or individual responsible for maintaining a recipe.
     */
    private final String maintainer;
    /**
     * The URL of an SVG format image which represents the maintainer's organization.
     */
    @Nullable
    private final URI logo;

    public Maintainer(final String maintainer, @Nullable final URI logo) {
        this.maintainer = maintainer;
        this.logo = logo;
    }

    /**
     * The name of the organization or individual responsible for maintaining a recipe.
     */
    public String getMaintainer() {
        return this.maintainer;
    }

    /**
     * The URL of an SVG format image which represents the maintainer's organization.
     */
    @Nullable
    public URI getLogo() {
        return this.logo;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof Maintainer)) return false;
        final Maintainer other = (Maintainer) o;
        final Object this$maintainer = this.getMaintainer();
        final Object other$maintainer = other.getMaintainer();
        if (this$maintainer == null ? other$maintainer != null : !this$maintainer.equals(other$maintainer)) return false;
        final Object this$logo = this.getLogo();
        final Object other$logo = other.getLogo();
        if (this$logo == null ? other$logo != null : !this$logo.equals(other$logo)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $maintainer = this.getMaintainer();
        result = result * PRIME + ($maintainer == null ? 43 : $maintainer.hashCode());
        final Object $logo = this.getLogo();
        result = result * PRIME + ($logo == null ? 43 : $logo.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "Maintainer(maintainer=" + this.getMaintainer() + ", logo=" + this.getLogo() + ")";
    }
}
