// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Per-compilation-unit structural/search inventory emitted by the Nebula M3 inventory recipe. */
public final class NebulaM3InventoryTable extends DataTable<NebulaM3InventoryTable.Row> {

    public NebulaM3InventoryTable(Recipe recipe) {
        super(
                recipe,
                "Nebula M3 Java inventory",
                "File-local structural, public-surface and fast-search signals. "
                        + "Rows are evidence only and grant no source-mutation authority.");
    }

    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative Java source path.")
        private final String sourcePath;

        @Column(displayName = "Module", description = "Path prefix before the source root.")
        private final String module;

        @Column(displayName = "Test source", description = "Whether the path is classified as test/example proof code.")
        private final boolean testSource;

        @Column(displayName = "Lines", description = "Printed compilation-unit line count.")
        private final int lineCount;

        @Column(displayName = "Types", description = "Class/interface/enum/record declarations.")
        private final int typeCount;

        @Column(displayName = "Public/protected types", description = "Externally visible type declarations.")
        private final int publicProtectedTypeCount;

        @Column(displayName = "Methods", description = "Method and constructor declarations.")
        private final int methodCount;

        @Column(displayName = "Public/protected methods", description = "Externally visible method declarations.")
        private final int publicProtectedMethodCount;

        @Column(displayName = "Native methods", description = "Native Java method declarations.")
        private final int nativeMethodCount;

        @Column(displayName = "Loops", description = "for/foreach/while/do-while statements.")
        private final int loopCount;

        @Column(displayName = "indexOf calls", description = "indexOf/lastIndexOf invocation count.")
        private final int indexOfCalls;

        @Column(displayName = "contains calls", description = "contains/containsKey/containsValue invocation count.")
        private final int containsCalls;

        @Column(displayName = "sort calls", description = "sort invocation count.")
        private final int sortCalls;

        @Column(displayName = "binarySearch calls", description = "binarySearch invocation count.")
        private final int binarySearchCalls;

        @Column(displayName = "TODO/FIXME markers", description = "Textual TODO/FIXME marker count.")
        private final int todoMarkers;

        @Column(
                displayName = "Method density x1000",
                description = "lineCount * 1000 / max(1, methodCount), integer-scaled.")
        private final int methodDensityX1000;

        @Column(
                displayName = "Public surface density x1000",
                description = "(visible types + visible methods) * 1000 / max(1, methods + types).")
        private final int publicSurfaceDensityX1000;

        @Column(displayName = "Fast-search signal", description = "Semicolon-separated structural search signals.")
        private final String fastSearchSignal;

        @Column(displayName = "Next pass", description = "Candidate-only next mechanical review action.")
        private final String recommendedNextPass;

        public Row(NebulaM3InventoryRecipe.SourceFacts facts) {
            this.sourcePath = facts.sourcePath();
            this.module = facts.module();
            this.testSource = facts.testSource();
            this.lineCount = facts.lineCount();
            this.typeCount = facts.typeCount();
            this.publicProtectedTypeCount = facts.publicProtectedTypeCount();
            this.methodCount = facts.methodCount();
            this.publicProtectedMethodCount = facts.publicProtectedMethodCount();
            this.nativeMethodCount = facts.nativeMethodCount();
            this.loopCount = facts.loopCount();
            this.indexOfCalls = facts.indexOfCalls();
            this.containsCalls = facts.containsCalls();
            this.sortCalls = facts.sortCalls();
            this.binarySearchCalls = facts.binarySearchCalls();
            this.todoMarkers = facts.todoMarkers();
            this.methodDensityX1000 = facts.methodDensityX1000();
            this.publicSurfaceDensityX1000 = facts.publicSurfaceDensityX1000();
            this.fastSearchSignal = facts.fastSearchSignal();
            this.recommendedNextPass = facts.recommendedNextPass();
        }

        public String getSourcePath() { return sourcePath; }
        public String getModule() { return module; }
        public boolean isTestSource() { return testSource; }
        public int getLineCount() { return lineCount; }
        public int getTypeCount() { return typeCount; }
        public int getPublicProtectedTypeCount() { return publicProtectedTypeCount; }
        public int getMethodCount() { return methodCount; }
        public int getPublicProtectedMethodCount() { return publicProtectedMethodCount; }
        public int getNativeMethodCount() { return nativeMethodCount; }
        public int getLoopCount() { return loopCount; }
        public int getIndexOfCalls() { return indexOfCalls; }
        public int getContainsCalls() { return containsCalls; }
        public int getSortCalls() { return sortCalls; }
        public int getBinarySearchCalls() { return binarySearchCalls; }
        public int getTodoMarkers() { return todoMarkers; }
        public int getMethodDensityX1000() { return methodDensityX1000; }
        public int getPublicSurfaceDensityX1000() { return publicSurfaceDensityX1000; }
        public String getFastSearchSignal() { return fastSearchSignal; }
        public String getRecommendedNextPass() { return recommendedNextPass; }
    }
}
