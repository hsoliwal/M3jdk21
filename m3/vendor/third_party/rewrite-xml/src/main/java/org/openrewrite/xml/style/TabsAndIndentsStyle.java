// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/style/TabsAndIndentsStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.xml.style;

import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

public final class TabsAndIndentsStyle implements XmlStyle {
    public static final TabsAndIndentsStyle DEFAULT = new TabsAndIndentsStyle(false, 2, 2, 4);
    private final Boolean useTabCharacter;
    private final Integer tabSize;
    private final Integer indentSize;
    private final Integer continuationIndentSize;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(DEFAULT, this);
    }

    public TabsAndIndentsStyle(final Boolean useTabCharacter, final Integer tabSize, final Integer indentSize, final Integer continuationIndentSize) {
        this.useTabCharacter = useTabCharacter;
        this.tabSize = tabSize;
        this.indentSize = indentSize;
        this.continuationIndentSize = continuationIndentSize;
    }

    public Boolean getUseTabCharacter() {
        return this.useTabCharacter;
    }

    public Integer getTabSize() {
        return this.tabSize;
    }

    public Integer getIndentSize() {
        return this.indentSize;
    }

    public Integer getContinuationIndentSize() {
        return this.continuationIndentSize;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TabsAndIndentsStyle)) return false;
        final TabsAndIndentsStyle other = (TabsAndIndentsStyle) o;
        final Object this$useTabCharacter = this.getUseTabCharacter();
        final Object other$useTabCharacter = other.getUseTabCharacter();
        if (this$useTabCharacter == null ? other$useTabCharacter != null : !this$useTabCharacter.equals(other$useTabCharacter)) return false;
        final Object this$tabSize = this.getTabSize();
        final Object other$tabSize = other.getTabSize();
        if (this$tabSize == null ? other$tabSize != null : !this$tabSize.equals(other$tabSize)) return false;
        final Object this$indentSize = this.getIndentSize();
        final Object other$indentSize = other.getIndentSize();
        if (this$indentSize == null ? other$indentSize != null : !this$indentSize.equals(other$indentSize)) return false;
        final Object this$continuationIndentSize = this.getContinuationIndentSize();
        final Object other$continuationIndentSize = other.getContinuationIndentSize();
        if (this$continuationIndentSize == null ? other$continuationIndentSize != null : !this$continuationIndentSize.equals(other$continuationIndentSize)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $useTabCharacter = this.getUseTabCharacter();
        result = result * PRIME + ($useTabCharacter == null ? 43 : $useTabCharacter.hashCode());
        final Object $tabSize = this.getTabSize();
        result = result * PRIME + ($tabSize == null ? 43 : $tabSize.hashCode());
        final Object $indentSize = this.getIndentSize();
        result = result * PRIME + ($indentSize == null ? 43 : $indentSize.hashCode());
        final Object $continuationIndentSize = this.getContinuationIndentSize();
        result = result * PRIME + ($continuationIndentSize == null ? 43 : $continuationIndentSize.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TabsAndIndentsStyle(useTabCharacter=" + this.getUseTabCharacter() + ", tabSize=" + this.getTabSize() + ", indentSize=" + this.getIndentSize() + ", continuationIndentSize=" + this.getContinuationIndentSize() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withUseTabCharacter(final Boolean useTabCharacter) {
        return this.useTabCharacter == useTabCharacter ? this : new TabsAndIndentsStyle(useTabCharacter, this.tabSize, this.indentSize, this.continuationIndentSize);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withTabSize(final Integer tabSize) {
        return this.tabSize == tabSize ? this : new TabsAndIndentsStyle(this.useTabCharacter, tabSize, this.indentSize, this.continuationIndentSize);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withIndentSize(final Integer indentSize) {
        return this.indentSize == indentSize ? this : new TabsAndIndentsStyle(this.useTabCharacter, this.tabSize, indentSize, this.continuationIndentSize);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withContinuationIndentSize(final Integer continuationIndentSize) {
        return this.continuationIndentSize == continuationIndentSize ? this : new TabsAndIndentsStyle(this.useTabCharacter, this.tabSize, this.indentSize, continuationIndentSize);
    }
}
