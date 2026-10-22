// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Per-method structural/search evidence emitted by the existing Nebula inventory owner. */
public final class NebulaM3MethodInventoryTable
        extends DataTable<NebulaM3MethodInventoryTable.Row> {

    public NebulaM3MethodInventoryTable(Recipe recipe) {
        super(
                recipe,
                "Nebula M3 method inventory",
                "Method-local structural/search/JNI evidence. Rows are read-only and "
                        + "grant no source or native execution authority.");
    }

    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative Java source path.")
        private final String sourcePath;
        @Column(displayName = "Owner type", description = "Typed or lexical owning class identity.")
        private final String ownerType;
        @Column(displayName = "Method key", description = "Stable typed or lexical method identity.")
        private final String methodKey;
        @Column(displayName = "Method name", description = "Simple method or constructor name.")
        private final String methodName;
        @Column(displayName = "Contract surface", description = "JNI/public/protected/internal classification.")
        private final String contractSurface;
        @Column(displayName = "Native method", description = "Whether this method is declared native.")
        private final boolean nativeMethod;
        @Column(displayName = "Direct statements", description = "Direct statements in the method body.")
        private final int directStatements;
        @Column(displayName = "Loops", description = "Loop nodes inside this method body.")
        private final int loopCount;
        @Column(displayName = "Branches", description = "if/switch branch nodes in this method body.")
        private final int branchCount;
        @Column(displayName = "Exception boundaries", description = "try/throw nodes in this method body.")
        private final int exceptionBoundaryCount;
        @Column(displayName = "Synchronization", description = "synchronized nodes in this method body.")
        private final int synchronizedCount;
        @Column(displayName = "Lambdas", description = "lambda nodes in this method body.")
        private final int lambdaCount;
        @Column(displayName = "Invocations", description = "Method invocation nodes in this method body.")
        private final int invocationCount;
        @Column(displayName = "indexOf calls", description = "indexOf/lastIndexOf calls in this method.")
        private final int indexOfCalls;
        @Column(displayName = "contains calls", description = "contains/containsKey/containsValue calls in this method.")
        private final int containsCalls;
        @Column(displayName = "sort calls", description = "sort calls in this method.")
        private final int sortCalls;
        @Column(displayName = "binarySearch calls", description = "binarySearch calls in this method.")
        private final int binarySearchCalls;
        @Column(displayName = "Structural patterns", description = "Sorted semicolon-separated method pattern labels.")
        private final String structuralPatterns;
        @Column(displayName = "Fast-search signal", description = "Method-local bounded search signal.")
        private final String fastSearchSignal;
        @Column(displayName = "Next pass", description = "Candidate-only next mechanical review action.")
        private final String recommendedNextPass;
        @Column(displayName = "Authority", description = "Always READ_ONLY_EVIDENCE.")
        private final String authority;

        Row(NebulaM3InventoryRecipe.MethodFacts facts) {
            this.sourcePath = facts.sourcePath();
            this.ownerType = facts.ownerType();
            this.methodKey = facts.methodKey();
            this.methodName = facts.methodName();
            this.contractSurface = facts.contractSurface();
            this.nativeMethod = facts.nativeMethod();
            this.directStatements = facts.directStatementCount();
            this.loopCount = facts.loopCount();
            this.branchCount = facts.branchCount();
            this.exceptionBoundaryCount = facts.exceptionBoundaryCount();
            this.synchronizedCount = facts.synchronizedCount();
            this.lambdaCount = facts.lambdaCount();
            this.invocationCount = facts.invocationCount();
            this.indexOfCalls = facts.indexOfCalls();
            this.containsCalls = facts.containsCalls();
            this.sortCalls = facts.sortCalls();
            this.binarySearchCalls = facts.binarySearchCalls();
            this.structuralPatterns = facts.structuralPatterns();
            this.fastSearchSignal = facts.fastSearchSignal();
            this.recommendedNextPass = facts.recommendedNextPass();
            this.authority = "READ_ONLY_EVIDENCE";
        }

        public String getSourcePath() { return sourcePath; }
        public String getOwnerType() { return ownerType; }
        public String getMethodKey() { return methodKey; }
        public String getMethodName() { return methodName; }
        public String getContractSurface() { return contractSurface; }
        public boolean isNativeMethod() { return nativeMethod; }
        public int getDirectStatements() { return directStatements; }
        public int getLoopCount() { return loopCount; }
        public int getBranchCount() { return branchCount; }
        public int getExceptionBoundaryCount() { return exceptionBoundaryCount; }
        public int getSynchronizedCount() { return synchronizedCount; }
        public int getLambdaCount() { return lambdaCount; }
        public int getInvocationCount() { return invocationCount; }
        public int getIndexOfCalls() { return indexOfCalls; }
        public int getContainsCalls() { return containsCalls; }
        public int getSortCalls() { return sortCalls; }
        public int getBinarySearchCalls() { return binarySearchCalls; }
        public String getStructuralPatterns() { return structuralPatterns; }
        public String getFastSearchSignal() { return fastSearchSignal; }
        public String getRecommendedNextPass() { return recommendedNextPass; }
        public String getAuthority() { return authority; }
    }
}
