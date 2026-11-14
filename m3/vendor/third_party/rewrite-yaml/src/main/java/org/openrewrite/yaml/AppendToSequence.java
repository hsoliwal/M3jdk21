// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/AppendToSequence.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.yaml;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import java.util.List;

public final class AppendToSequence extends Recipe {
    @Option(displayName = "sequence path", description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML sequence.", example = "$.universe.planets")
    private final String sequencePath;
    @Option(displayName = "New value", description = "The new value to be appended to the sequence.", example = "earth")
    private final String value;
    @Option(displayName = "Optional: match existing sequence values", description = "If specified, the item will only be appended if the existing sequence matches these values.", example = "existingValue1", required = false)
    @Nullable
    private final List<String> existingSequenceValues;
    @Option(displayName = "Optional: match existing sequence values in any order", description = "If specified in combination with the above parameter, the item will only be appended if the existing sequence has the specified values in any order.", example = "true", required = false)
    @Nullable
    private final Boolean matchExistingSequenceValuesInAnyOrder;

    @Override
    public String getDisplayName() {
        return "Append to sequence";
    }

    @Override
    public String getInstanceName() {
        return String.format("Append %s to sequence `%s`", value, sequencePath);
    }

    @Override
    public String getDescription() {
        return "Append item to YAML sequence.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(sequencePath);
        return new AppendToSequenceVisitor(matcher, value, existingSequenceValues, Boolean.TRUE.equals(matchExistingSequenceValuesInAnyOrder));
    }

    public AppendToSequence(final String sequencePath, final String value, @Nullable final List<String> existingSequenceValues, @Nullable final Boolean matchExistingSequenceValuesInAnyOrder) {
        this.sequencePath = sequencePath;
        this.value = value;
        this.existingSequenceValues = existingSequenceValues;
        this.matchExistingSequenceValuesInAnyOrder = matchExistingSequenceValuesInAnyOrder;
    }

    public String getSequencePath() {
        return this.sequencePath;
    }

    public String getValue() {
        return this.value;
    }

    @Nullable
    public List<String> getExistingSequenceValues() {
        return this.existingSequenceValues;
    }

    @Nullable
    public Boolean getMatchExistingSequenceValuesInAnyOrder() {
        return this.matchExistingSequenceValuesInAnyOrder;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "AppendToSequence(sequencePath=" + this.getSequencePath() + ", value=" + this.getValue() + ", existingSequenceValues=" + this.getExistingSequenceValues() + ", matchExistingSequenceValuesInAnyOrder=" + this.getMatchExistingSequenceValuesInAnyOrder() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof AppendToSequence)) return false;
        final AppendToSequence other = (AppendToSequence) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchExistingSequenceValuesInAnyOrder = this.getMatchExistingSequenceValuesInAnyOrder();
        final Object other$matchExistingSequenceValuesInAnyOrder = other.getMatchExistingSequenceValuesInAnyOrder();
        if (this$matchExistingSequenceValuesInAnyOrder == null ? other$matchExistingSequenceValuesInAnyOrder != null : !this$matchExistingSequenceValuesInAnyOrder.equals(other$matchExistingSequenceValuesInAnyOrder)) return false;
        final Object this$sequencePath = this.getSequencePath();
        final Object other$sequencePath = other.getSequencePath();
        if (this$sequencePath == null ? other$sequencePath != null : !this$sequencePath.equals(other$sequencePath)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        final Object this$existingSequenceValues = this.getExistingSequenceValues();
        final Object other$existingSequenceValues = other.getExistingSequenceValues();
        if (this$existingSequenceValues == null ? other$existingSequenceValues != null : !this$existingSequenceValues.equals(other$existingSequenceValues)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof AppendToSequence;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchExistingSequenceValuesInAnyOrder = this.getMatchExistingSequenceValuesInAnyOrder();
        result = result * PRIME + ($matchExistingSequenceValuesInAnyOrder == null ? 43 : $matchExistingSequenceValuesInAnyOrder.hashCode());
        final Object $sequencePath = this.getSequencePath();
        result = result * PRIME + ($sequencePath == null ? 43 : $sequencePath.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        final Object $existingSequenceValues = this.getExistingSequenceValues();
        result = result * PRIME + ($existingSequenceValues == null ? 43 : $existingSequenceValues.hashCode());
        return result;
    }
}
