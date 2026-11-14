// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/LstProvenanceTable.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.LstProvenance;

@JsonIgnoreType
public class LstProvenanceTable extends DataTable<LstProvenanceTable.Row> {
    public LstProvenanceTable(Recipe recipe) {
        super(recipe, "LST provenance", "Table showing which tools were used to produce LSTs.");
    }


    public static final class Row {
        @Column(displayName = "Build tool type", description = "The type of tool which produced the LST.")
        private final LstProvenance.Type buildToolType;
        @Column(displayName = "Build tool version", description = "The version of the build tool which produced the LST.")
        private final String buildToolVersion;
        @Column(displayName = "LST serializer version", description = "The version of LST serializer which produced the LST.")
        private final String lstSerializerVersion;
        @Column(displayName = "Timestamp (epoch millis)", description = "UTC timestamp describing when the LST was produced, in milliseconds since the unix epoch.")
        @Nullable
        private final Long timestampEpochMillis;
        @Column(displayName = "Timestamp", description = "UTC timestamp describing when the LST was produced, in ISO-8601 format. e.g.: \"2023‐08‐07T22:24:06+00:00 UTC+00:00\"")
        @Nullable
        private final String timestampUtc;

        public Row(final LstProvenance.Type buildToolType, final String buildToolVersion, final String lstSerializerVersion, @Nullable final Long timestampEpochMillis, @Nullable final String timestampUtc) {
            this.buildToolType = buildToolType;
            this.buildToolVersion = buildToolVersion;
            this.lstSerializerVersion = lstSerializerVersion;
            this.timestampEpochMillis = timestampEpochMillis;
            this.timestampUtc = timestampUtc;
        }

        public LstProvenance.Type getBuildToolType() {
            return this.buildToolType;
        }

        public String getBuildToolVersion() {
            return this.buildToolVersion;
        }

        public String getLstSerializerVersion() {
            return this.lstSerializerVersion;
        }

        @Nullable
        public Long getTimestampEpochMillis() {
            return this.timestampEpochMillis;
        }

        @Nullable
        public String getTimestampUtc() {
            return this.timestampUtc;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof LstProvenanceTable.Row)) return false;
            final LstProvenanceTable.Row other = (LstProvenanceTable.Row) o;
            final Object this$timestampEpochMillis = this.getTimestampEpochMillis();
            final Object other$timestampEpochMillis = other.getTimestampEpochMillis();
            if (this$timestampEpochMillis == null ? other$timestampEpochMillis != null : !this$timestampEpochMillis.equals(other$timestampEpochMillis)) return false;
            final Object this$buildToolType = this.getBuildToolType();
            final Object other$buildToolType = other.getBuildToolType();
            if (this$buildToolType == null ? other$buildToolType != null : !this$buildToolType.equals(other$buildToolType)) return false;
            final Object this$buildToolVersion = this.getBuildToolVersion();
            final Object other$buildToolVersion = other.getBuildToolVersion();
            if (this$buildToolVersion == null ? other$buildToolVersion != null : !this$buildToolVersion.equals(other$buildToolVersion)) return false;
            final Object this$lstSerializerVersion = this.getLstSerializerVersion();
            final Object other$lstSerializerVersion = other.getLstSerializerVersion();
            if (this$lstSerializerVersion == null ? other$lstSerializerVersion != null : !this$lstSerializerVersion.equals(other$lstSerializerVersion)) return false;
            final Object this$timestampUtc = this.getTimestampUtc();
            final Object other$timestampUtc = other.getTimestampUtc();
            if (this$timestampUtc == null ? other$timestampUtc != null : !this$timestampUtc.equals(other$timestampUtc)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $timestampEpochMillis = this.getTimestampEpochMillis();
            result = result * PRIME + ($timestampEpochMillis == null ? 43 : $timestampEpochMillis.hashCode());
            final Object $buildToolType = this.getBuildToolType();
            result = result * PRIME + ($buildToolType == null ? 43 : $buildToolType.hashCode());
            final Object $buildToolVersion = this.getBuildToolVersion();
            result = result * PRIME + ($buildToolVersion == null ? 43 : $buildToolVersion.hashCode());
            final Object $lstSerializerVersion = this.getLstSerializerVersion();
            result = result * PRIME + ($lstSerializerVersion == null ? 43 : $lstSerializerVersion.hashCode());
            final Object $timestampUtc = this.getTimestampUtc();
            result = result * PRIME + ($timestampUtc == null ? 43 : $timestampUtc.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "LstProvenanceTable.Row(buildToolType=" + this.getBuildToolType() + ", buildToolVersion=" + this.getBuildToolVersion() + ", lstSerializerVersion=" + this.getLstSerializerVersion() + ", timestampEpochMillis=" + this.getTimestampEpochMillis() + ", timestampUtc=" + this.getTimestampUtc() + ")";
        }
    }
}
