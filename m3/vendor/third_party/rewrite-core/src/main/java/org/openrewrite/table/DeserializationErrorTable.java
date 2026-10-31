// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/DeserializationErrorTable.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2024 the original author or authors.
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

@JsonIgnoreType
public class DeserializationErrorTable extends DataTable<DeserializationErrorTable.Row> {
    public DeserializationErrorTable(Recipe recipe) {
        super(recipe, "Deserialization errors", "Table collecting any LST deserialization errors.");
    }


    public static final class Row {
        @Column(displayName = "Source path", description = "The source that failed to be deserialized.")
        private final String sourcePath;
        @Column(displayName = "Error message", description = "The error message of the exception that produced the error.")
        @Nullable
        private final String errorMessage;
        @Column(displayName = "Stack trace", description = "The stack trace of the error.")
        private final String stackTrace;
        @Column(displayName = "Language", description = "Language of source file in case it requires a newer CLI version for the ingestion.")
        @Nullable
        private final String language;
        @Column(displayName = "Minimum CLI version required by language", description = "Minimum CLI version required to be able to successfully ingest sources of this language.")
        @Nullable
        private final String minimumVersion;
        @Column(displayName = "Actual CLI version used for ingestion", description = "Actual CLI version used to ingest this source.")
        @Nullable
        private final String actualVersion;
        @Column(displayName = "Timestamp (epoch millis)", description = "UTC timestamp describing when the LST was produced, in milliseconds since the unix epoch.")
        @Nullable
        private final Long timestampEpochMillis;
        @Column(displayName = "Timestamp", description = "UTC timestamp describing when the LST was produced, in ISO-8601 format. e.g.: \"2023‐08‐07T22:24:06+00:00 UTC+00:00\"")
        @Nullable
        private final String timestampUtc;

        public Row(final String sourcePath, @Nullable final String errorMessage, final String stackTrace, @Nullable final String language, @Nullable final String minimumVersion, @Nullable final String actualVersion, @Nullable final Long timestampEpochMillis, @Nullable final String timestampUtc) {
            this.sourcePath = sourcePath;
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
            this.language = language;
            this.minimumVersion = minimumVersion;
            this.actualVersion = actualVersion;
            this.timestampEpochMillis = timestampEpochMillis;
            this.timestampUtc = timestampUtc;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        @Nullable
        public String getErrorMessage() {
            return this.errorMessage;
        }

        public String getStackTrace() {
            return this.stackTrace;
        }

        @Nullable
        public String getLanguage() {
            return this.language;
        }

        @Nullable
        public String getMinimumVersion() {
            return this.minimumVersion;
        }

        @Nullable
        public String getActualVersion() {
            return this.actualVersion;
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
            if (!(o instanceof DeserializationErrorTable.Row)) return false;
            final DeserializationErrorTable.Row other = (DeserializationErrorTable.Row) o;
            final Object this$timestampEpochMillis = this.getTimestampEpochMillis();
            final Object other$timestampEpochMillis = other.getTimestampEpochMillis();
            if (this$timestampEpochMillis == null ? other$timestampEpochMillis != null : !this$timestampEpochMillis.equals(other$timestampEpochMillis)) return false;
            final Object this$sourcePath = this.getSourcePath();
            final Object other$sourcePath = other.getSourcePath();
            if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
            final Object this$errorMessage = this.getErrorMessage();
            final Object other$errorMessage = other.getErrorMessage();
            if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
            final Object this$stackTrace = this.getStackTrace();
            final Object other$stackTrace = other.getStackTrace();
            if (this$stackTrace == null ? other$stackTrace != null : !this$stackTrace.equals(other$stackTrace)) return false;
            final Object this$language = this.getLanguage();
            final Object other$language = other.getLanguage();
            if (this$language == null ? other$language != null : !this$language.equals(other$language)) return false;
            final Object this$minimumVersion = this.getMinimumVersion();
            final Object other$minimumVersion = other.getMinimumVersion();
            if (this$minimumVersion == null ? other$minimumVersion != null : !this$minimumVersion.equals(other$minimumVersion)) return false;
            final Object this$actualVersion = this.getActualVersion();
            final Object other$actualVersion = other.getActualVersion();
            if (this$actualVersion == null ? other$actualVersion != null : !this$actualVersion.equals(other$actualVersion)) return false;
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
            final Object $sourcePath = this.getSourcePath();
            result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
            final Object $errorMessage = this.getErrorMessage();
            result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
            final Object $stackTrace = this.getStackTrace();
            result = result * PRIME + ($stackTrace == null ? 43 : $stackTrace.hashCode());
            final Object $language = this.getLanguage();
            result = result * PRIME + ($language == null ? 43 : $language.hashCode());
            final Object $minimumVersion = this.getMinimumVersion();
            result = result * PRIME + ($minimumVersion == null ? 43 : $minimumVersion.hashCode());
            final Object $actualVersion = this.getActualVersion();
            result = result * PRIME + ($actualVersion == null ? 43 : $actualVersion.hashCode());
            final Object $timestampUtc = this.getTimestampUtc();
            result = result * PRIME + ($timestampUtc == null ? 43 : $timestampUtc.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DeserializationErrorTable.Row(sourcePath=" + this.getSourcePath() + ", errorMessage=" + this.getErrorMessage() + ", stackTrace=" + this.getStackTrace() + ", language=" + this.getLanguage() + ", minimumVersion=" + this.getMinimumVersion() + ", actualVersion=" + this.getActualVersion() + ", timestampEpochMillis=" + this.getTimestampEpochMillis() + ", timestampUtc=" + this.getTimestampUtc() + ")";
        }
    }
}
