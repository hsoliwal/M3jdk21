// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/HiddenFieldStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.style;

import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;
import static org.openrewrite.java.style.Checkstyle.hiddenFieldStyle;

public final class HiddenFieldStyle implements Style {
    /**
     * Whether to ignore constructor parameters.
     */
    private final Boolean ignoreConstructorParameter;
    /**
     * Whether to ignore the parameter of a property setter method.
     */
    private final Boolean ignoreSetter;
    /**
     * Whether to expand the definition of a setter method to include methods that return the class' instance.
     * This only has an effect if {@link HiddenFieldStyle#ignoreSetter} is set to true.
     */
    private final Boolean setterCanReturnItsClass;
    /**
     * Whether to ignore parameters of abstract methods.
     */
    private final Boolean ignoreAbstractMethods;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(hiddenFieldStyle(), this);
    }

    public HiddenFieldStyle(final Boolean ignoreConstructorParameter, final Boolean ignoreSetter, final Boolean setterCanReturnItsClass, final Boolean ignoreAbstractMethods) {
        this.ignoreConstructorParameter = ignoreConstructorParameter;
        this.ignoreSetter = ignoreSetter;
        this.setterCanReturnItsClass = setterCanReturnItsClass;
        this.ignoreAbstractMethods = ignoreAbstractMethods;
    }

    /**
     * Whether to ignore constructor parameters.
     */
    public Boolean getIgnoreConstructorParameter() {
        return this.ignoreConstructorParameter;
    }

    /**
     * Whether to ignore the parameter of a property setter method.
     */
    public Boolean getIgnoreSetter() {
        return this.ignoreSetter;
    }

    /**
     * Whether to expand the definition of a setter method to include methods that return the class' instance.
     * This only has an effect if {@link HiddenFieldStyle#ignoreSetter} is set to true.
     */
    public Boolean getSetterCanReturnItsClass() {
        return this.setterCanReturnItsClass;
    }

    /**
     * Whether to ignore parameters of abstract methods.
     */
    public Boolean getIgnoreAbstractMethods() {
        return this.ignoreAbstractMethods;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof HiddenFieldStyle)) return false;
        final HiddenFieldStyle other = (HiddenFieldStyle) o;
        final Object this$ignoreConstructorParameter = this.getIgnoreConstructorParameter();
        final Object other$ignoreConstructorParameter = other.getIgnoreConstructorParameter();
        if (this$ignoreConstructorParameter == null ? other$ignoreConstructorParameter != null : !this$ignoreConstructorParameter.equals(other$ignoreConstructorParameter)) return false;
        final Object this$ignoreSetter = this.getIgnoreSetter();
        final Object other$ignoreSetter = other.getIgnoreSetter();
        if (this$ignoreSetter == null ? other$ignoreSetter != null : !this$ignoreSetter.equals(other$ignoreSetter)) return false;
        final Object this$setterCanReturnItsClass = this.getSetterCanReturnItsClass();
        final Object other$setterCanReturnItsClass = other.getSetterCanReturnItsClass();
        if (this$setterCanReturnItsClass == null ? other$setterCanReturnItsClass != null : !this$setterCanReturnItsClass.equals(other$setterCanReturnItsClass)) return false;
        final Object this$ignoreAbstractMethods = this.getIgnoreAbstractMethods();
        final Object other$ignoreAbstractMethods = other.getIgnoreAbstractMethods();
        if (this$ignoreAbstractMethods == null ? other$ignoreAbstractMethods != null : !this$ignoreAbstractMethods.equals(other$ignoreAbstractMethods)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $ignoreConstructorParameter = this.getIgnoreConstructorParameter();
        result = result * PRIME + ($ignoreConstructorParameter == null ? 43 : $ignoreConstructorParameter.hashCode());
        final Object $ignoreSetter = this.getIgnoreSetter();
        result = result * PRIME + ($ignoreSetter == null ? 43 : $ignoreSetter.hashCode());
        final Object $setterCanReturnItsClass = this.getSetterCanReturnItsClass();
        result = result * PRIME + ($setterCanReturnItsClass == null ? 43 : $setterCanReturnItsClass.hashCode());
        final Object $ignoreAbstractMethods = this.getIgnoreAbstractMethods();
        result = result * PRIME + ($ignoreAbstractMethods == null ? 43 : $ignoreAbstractMethods.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "HiddenFieldStyle(ignoreConstructorParameter=" + this.getIgnoreConstructorParameter() + ", ignoreSetter=" + this.getIgnoreSetter() + ", setterCanReturnItsClass=" + this.getSetterCanReturnItsClass() + ", ignoreAbstractMethods=" + this.getIgnoreAbstractMethods() + ")";
    }

    /**
     * Whether to ignore constructor parameters.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public HiddenFieldStyle withIgnoreConstructorParameter(final Boolean ignoreConstructorParameter) {
        return this.ignoreConstructorParameter == ignoreConstructorParameter ? this : new HiddenFieldStyle(ignoreConstructorParameter, this.ignoreSetter, this.setterCanReturnItsClass, this.ignoreAbstractMethods);
    }

    /**
     * Whether to ignore the parameter of a property setter method.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public HiddenFieldStyle withIgnoreSetter(final Boolean ignoreSetter) {
        return this.ignoreSetter == ignoreSetter ? this : new HiddenFieldStyle(this.ignoreConstructorParameter, ignoreSetter, this.setterCanReturnItsClass, this.ignoreAbstractMethods);
    }

    /**
     * Whether to expand the definition of a setter method to include methods that return the class' instance.
     * This only has an effect if {@link HiddenFieldStyle#ignoreSetter} is set to true.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public HiddenFieldStyle withSetterCanReturnItsClass(final Boolean setterCanReturnItsClass) {
        return this.setterCanReturnItsClass == setterCanReturnItsClass ? this : new HiddenFieldStyle(this.ignoreConstructorParameter, this.ignoreSetter, setterCanReturnItsClass, this.ignoreAbstractMethods);
    }

    /**
     * Whether to ignore parameters of abstract methods.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public HiddenFieldStyle withIgnoreAbstractMethods(final Boolean ignoreAbstractMethods) {
        return this.ignoreAbstractMethods == ignoreAbstractMethods ? this : new HiddenFieldStyle(this.ignoreConstructorParameter, this.ignoreSetter, this.setterCanReturnItsClass, ignoreAbstractMethods);
    }
}
