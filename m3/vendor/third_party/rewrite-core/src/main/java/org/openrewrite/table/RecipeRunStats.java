// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/RecipeRunStats.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import static java.util.Objects.requireNonNull;

public class RecipeRunStats extends DataTable<RecipeRunStats.Row> {
    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final Set<Path> sourceFileChanged = new HashSet<>();

    public RecipeRunStats(Recipe recipe) {
        super(recipe, "Recipe performance", "Statistics used in analyzing the performance of recipes.");
    }

    public void recordSourceFileChanged(@Nullable SourceFile before, @Nullable SourceFile after) {
        if (after != null) {
            sourceFileChanged.add(after.getSourcePath());
        } else if (before != null) {
            sourceFileChanged.add(before.getSourcePath());
        }
    }

    public void recordScan(Recipe recipe, Callable<SourceFile> scan) throws Exception {
        Timer.builder("rewrite.recipe.scan").tag("name", recipe.getName()).publishPercentiles(0.99).register(registry).recordCallable(scan);
    }

    @Nullable
    public SourceFile recordEdit(Recipe recipe, Callable<SourceFile> edit) throws Exception {
        return Timer.builder("rewrite.recipe.edit").tag("name", recipe.getName()).publishPercentiles(0.99).register(registry).recordCallable(edit);
    }

    public void flush(ExecutionContext ctx) {
        for (Timer editor : registry.find("rewrite.recipe.edit").timers()) {
            String recipeName = requireNonNull(editor.getId().getTag("name"));
            Timer scanner = registry.find("rewrite.recipe.scan").tag("name", recipeName).timer();
            Row row = new Row(recipeName, Long.valueOf(editor.count()).intValue(), sourceFileChanged.size(), scanner == null ? 0 : (long) scanner.totalTime(TimeUnit.NANOSECONDS), scanner == null ? 0 : scanner.takeSnapshot().percentileValues()[0].percentile(), scanner == null ? 0 : (long) scanner.max(TimeUnit.NANOSECONDS), (long) editor.totalTime(TimeUnit.NANOSECONDS), editor.takeSnapshot().percentileValues()[0].percentile(), (long) editor.max(TimeUnit.NANOSECONDS));
            //noinspection DuplicatedCode
            ctx.computeMessage(ExecutionContext.DATA_TABLES, row, ConcurrentHashMap::new, (extract, allDataTables) -> {
                //noinspection unchecked
                List<Row> dataTablesOfType = (List<Row>) allDataTables.computeIfAbsent(this, c -> new ArrayList<>());
                dataTablesOfType.add(row);
                return allDataTables;
            });
        }
    }


    public static final class Row {
        @Column(displayName = "The recipe", description = "The recipe whose stats are being measured both individually and cumulatively.")
        private final String recipe;
        @Column(displayName = "Source file count", description = "The number of source files the recipe ran over.")
        private final Integer sourceFiles;
        @Column(displayName = "Source file changed count", description = "The number of source files which were changed in the recipe run. Includes files created, deleted, and edited.")
        private final Integer sourceFilesChanged;
        @Column(displayName = "Cumulative scanning time", description = "The total time spent across the scanning phase of this recipe.")
        private final Long scanTotalTime;
        @Column(displayName = "99th percentile scanning time", description = "99 out of 100 scans completed in this amount of time.")
        private final Double scanP99;
        @Column(displayName = "Max scanning time", description = "The max time scanning any one source file.")
        private final Long scanMax;
        @Column(displayName = "Cumulative edit time", description = "The total time spent across the editing phase of this recipe.")
        private final Long editTotalTime;
        @Column(displayName = "99th percentile edit time", description = "99 out of 100 edits completed in this amount of time.")
        private final Double editP99;
        @Column(displayName = "Max edit time", description = "The max time editing any one source file.")
        private final Long editMax;

        public Row(final String recipe, final Integer sourceFiles, final Integer sourceFilesChanged, final Long scanTotalTime, final Double scanP99, final Long scanMax, final Long editTotalTime, final Double editP99, final Long editMax) {
            this.recipe = recipe;
            this.sourceFiles = sourceFiles;
            this.sourceFilesChanged = sourceFilesChanged;
            this.scanTotalTime = scanTotalTime;
            this.scanP99 = scanP99;
            this.scanMax = scanMax;
            this.editTotalTime = editTotalTime;
            this.editP99 = editP99;
            this.editMax = editMax;
        }

        public String getRecipe() {
            return this.recipe;
        }

        public Integer getSourceFiles() {
            return this.sourceFiles;
        }

        public Integer getSourceFilesChanged() {
            return this.sourceFilesChanged;
        }

        public Long getScanTotalTime() {
            return this.scanTotalTime;
        }

        public Double getScanP99() {
            return this.scanP99;
        }

        public Long getScanMax() {
            return this.scanMax;
        }

        public Long getEditTotalTime() {
            return this.editTotalTime;
        }

        public Double getEditP99() {
            return this.editP99;
        }

        public Long getEditMax() {
            return this.editMax;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof RecipeRunStats.Row)) return false;
            final RecipeRunStats.Row other = (RecipeRunStats.Row) o;
            final Object this$sourceFiles = this.getSourceFiles();
            final Object other$sourceFiles = other.getSourceFiles();
            if (this$sourceFiles == null ? other$sourceFiles != null : !this$sourceFiles.equals(other$sourceFiles)) return false;
            final Object this$sourceFilesChanged = this.getSourceFilesChanged();
            final Object other$sourceFilesChanged = other.getSourceFilesChanged();
            if (this$sourceFilesChanged == null ? other$sourceFilesChanged != null : !this$sourceFilesChanged.equals(other$sourceFilesChanged)) return false;
            final Object this$scanTotalTime = this.getScanTotalTime();
            final Object other$scanTotalTime = other.getScanTotalTime();
            if (this$scanTotalTime == null ? other$scanTotalTime != null : !this$scanTotalTime.equals(other$scanTotalTime)) return false;
            final Object this$scanP99 = this.getScanP99();
            final Object other$scanP99 = other.getScanP99();
            if (this$scanP99 == null ? other$scanP99 != null : !this$scanP99.equals(other$scanP99)) return false;
            final Object this$scanMax = this.getScanMax();
            final Object other$scanMax = other.getScanMax();
            if (this$scanMax == null ? other$scanMax != null : !this$scanMax.equals(other$scanMax)) return false;
            final Object this$editTotalTime = this.getEditTotalTime();
            final Object other$editTotalTime = other.getEditTotalTime();
            if (this$editTotalTime == null ? other$editTotalTime != null : !this$editTotalTime.equals(other$editTotalTime)) return false;
            final Object this$editP99 = this.getEditP99();
            final Object other$editP99 = other.getEditP99();
            if (this$editP99 == null ? other$editP99 != null : !this$editP99.equals(other$editP99)) return false;
            final Object this$editMax = this.getEditMax();
            final Object other$editMax = other.getEditMax();
            if (this$editMax == null ? other$editMax != null : !this$editMax.equals(other$editMax)) return false;
            final Object this$recipe = this.getRecipe();
            final Object other$recipe = other.getRecipe();
            if (this$recipe == null ? other$recipe != null : !this$recipe.equals(other$recipe)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $sourceFiles = this.getSourceFiles();
            result = result * PRIME + ($sourceFiles == null ? 43 : $sourceFiles.hashCode());
            final Object $sourceFilesChanged = this.getSourceFilesChanged();
            result = result * PRIME + ($sourceFilesChanged == null ? 43 : $sourceFilesChanged.hashCode());
            final Object $scanTotalTime = this.getScanTotalTime();
            result = result * PRIME + ($scanTotalTime == null ? 43 : $scanTotalTime.hashCode());
            final Object $scanP99 = this.getScanP99();
            result = result * PRIME + ($scanP99 == null ? 43 : $scanP99.hashCode());
            final Object $scanMax = this.getScanMax();
            result = result * PRIME + ($scanMax == null ? 43 : $scanMax.hashCode());
            final Object $editTotalTime = this.getEditTotalTime();
            result = result * PRIME + ($editTotalTime == null ? 43 : $editTotalTime.hashCode());
            final Object $editP99 = this.getEditP99();
            result = result * PRIME + ($editP99 == null ? 43 : $editP99.hashCode());
            final Object $editMax = this.getEditMax();
            result = result * PRIME + ($editMax == null ? 43 : $editMax.hashCode());
            final Object $recipe = this.getRecipe();
            result = result * PRIME + ($recipe == null ? 43 : $recipe.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "RecipeRunStats.Row(recipe=" + this.getRecipe() + ", sourceFiles=" + this.getSourceFiles() + ", sourceFilesChanged=" + this.getSourceFilesChanged() + ", scanTotalTime=" + this.getScanTotalTime() + ", scanP99=" + this.getScanP99() + ", scanMax=" + this.getScanMax() + ", editTotalTime=" + this.getEditTotalTime() + ", editP99=" + this.getEditP99() + ", editMax=" + this.getEditMax() + ")";
        }
    }
}
