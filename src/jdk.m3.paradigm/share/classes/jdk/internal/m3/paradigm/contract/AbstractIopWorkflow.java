/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
/**
 * Template Method base for {@link IopWorkflow}.
 *
 * <p>
 * Delegates workflow operations to {@code doXxx()} methods with lifecycle
 * hooks.
 *
 * @param <Self>
 *            self type for fluent API
 * @since 1.1.0
 * @m3.pattern synexia:bop
 * @m3.algorithm Declares final wrappers for scope, contract, documentation, implementation, compile and test phases. Each requires a nonnull input before dispatching its before hook, abstract phase implementation and after hook, then passes the result reference through without validation or finally-based dispatch.
 * @m3.dataStructures Declared class owner with no directly declared fields. Nested types and inherited members keep their own declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AbstractIopWorkflow.java}
 * blob {@code eb2950c2596c0c890ff3bd5da5a46f7845aa2085} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractIopWorkflow<Self extends AbstractIopWorkflow<Self>> implements IopWorkflow {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractIopWorkflow.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public AbstractIopWorkflow() {
    }


    // --- TEMPLATE METHODS ---

    /**
     * lockScope operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Requires nonnull path, calls onBeforeLockScope, invokes doLockScope with the same input reference, calls onAfterLockScope with the original input and returned result, then returns that result reference. Null results are passed through, and exceptions skip remaining calls because there is no finally block.
     * @m3.dataStructures Declared return type ScopeResult with formal parameters Path path. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    @Override
    public final ScopeResult lockScope(Path path) {
        requireNonNull(path, "path");
        onBeforeLockScope(path);
        ScopeResult result = doLockScope(path);
        onAfterLockScope(path, result);
        return result;
    }

    /**
     * readContracts operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Requires nonnull scope, calls onBeforeReadContracts, invokes doReadContracts with the same input reference, calls onAfterReadContracts with the original input and returned result, then returns that result reference. Null results are passed through, and exceptions skip remaining calls because there is no finally block.
     * @m3.dataStructures Declared return type ContractAnalysis with formal parameters ScopeResult scope. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    @Override
    public final ContractAnalysis readContracts(ScopeResult scope) {
        requireNonNull(scope, "scope");
        onBeforeReadContracts(scope);
        ContractAnalysis result = doReadContracts(scope);
        onAfterReadContracts(scope, result);
        return result;
    }

    /**
     * repairDocumentation operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Requires nonnull analysis, calls onBeforeRepairDocumentation, invokes doRepairDocumentation with the same input reference, calls onAfterRepairDocumentation with the original input and returned result, then returns that result reference. Null results are passed through, and exceptions skip remaining calls because there is no finally block.
     * @m3.dataStructures Declared return type DocRepairResult with formal parameters ContractAnalysis analysis. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    @Override
    public final DocRepairResult repairDocumentation(ContractAnalysis analysis) {
        requireNonNull(analysis, "analysis");
        onBeforeRepairDocumentation(analysis);
        DocRepairResult result = doRepairDocumentation(analysis);
        onAfterRepairDocumentation(analysis, result);
        return result;
    }

    /**
     * implement operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Requires nonnull analysis, calls onBeforeImplement, invokes doImplement with the same input reference, calls onAfterImplement with the original input and returned result, then returns that result reference. Null results are passed through, and exceptions skip remaining calls because there is no finally block.
     * @m3.dataStructures Declared return type ImplementResult with formal parameters ContractAnalysis analysis. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    @Override
    public final ImplementResult implement(ContractAnalysis analysis) {
        requireNonNull(analysis, "analysis");
        onBeforeImplement(analysis);
        ImplementResult result = doImplement(analysis);
        onAfterImplement(analysis, result);
        return result;
    }

    /**
     * compile operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Requires nonnull implementation, calls onBeforeCompile, invokes doCompile with the same input reference, calls onAfterCompile with the original input and returned result, then returns that result reference. Null results are passed through, and exceptions skip remaining calls because there is no finally block.
     * @m3.dataStructures Declared return type CompileResult with formal parameters ImplementResult implementation. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    @Override
    public final CompileResult compile(ImplementResult implementation) {
        requireNonNull(implementation, "implementation");
        onBeforeCompile(implementation);
        CompileResult result = doCompile(implementation);
        onAfterCompile(implementation, result);
        return result;
    }

    /**
     * test operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Requires nonnull compiled, calls onBeforeTest, invokes doTest with the same input reference, calls onAfterTest with the original input and returned result, then returns that result reference. Null results are passed through, and exceptions skip remaining calls because there is no finally block.
     * @m3.dataStructures Declared return type TestResult with formal parameters CompileResult compiled. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    @Override
    public final TestResult test(CompileResult compiled) {
        requireNonNull(compiled, "compiled");
        onBeforeTest(compiled);
        TestResult result = doTest(compiled);
        onAfterTest(compiled, result);
        return result;
    }

    // --- ABSTRACT doXxx() - SUBCLASS PROVIDES ---

    /**
     * doLockScope operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body, and the concrete implementation supplies its algorithm.; review=generator-declarations:local-review/AbstractIopWorkflow/2957
     * @m3.dataStructures Declared return type ScopeResult with formal parameters Path path. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected abstract ScopeResult doLockScope(Path path);

    /**
     * doReadContracts operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body, and the concrete implementation supplies its algorithm.; review=generator-declarations:local-review/AbstractIopWorkflow/3017
     * @m3.dataStructures Declared return type ContractAnalysis with formal parameters ScopeResult scope. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected abstract ContractAnalysis doReadContracts(ScopeResult scope);

    /**
     * doRepairDocumentation operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body, and the concrete implementation supplies its algorithm.; review=generator-declarations:local-review/AbstractIopWorkflow/3094
     * @m3.dataStructures Declared return type DocRepairResult with formal parameters ContractAnalysis analysis. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected abstract DocRepairResult doRepairDocumentation(ContractAnalysis analysis);

    /**
     * doImplement operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body, and the concrete implementation supplies its algorithm.; review=generator-declarations:local-review/AbstractIopWorkflow/3184
     * @m3.dataStructures Declared return type ImplementResult with formal parameters ContractAnalysis analysis. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected abstract ImplementResult doImplement(ContractAnalysis analysis);

    /**
     * doCompile operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body, and the concrete implementation supplies its algorithm.; review=generator-declarations:local-review/AbstractIopWorkflow/3264
     * @m3.dataStructures Declared return type CompileResult with formal parameters ImplementResult implementation. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected abstract CompileResult doCompile(ImplementResult implementation);

    /**
     * doTest operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body, and the concrete implementation supplies its algorithm.; review=generator-declarations:local-review/AbstractIopWorkflow/3345
     * @m3.dataStructures Declared return type TestResult with formal parameters CompileResult compiled. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected abstract TestResult doTest(CompileResult compiled);

    // --- CALLBACK HOOKS - onXxx() ---

    /**
     * onBeforeLockScope operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters Path path. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onBeforeLockScope(Path path) {}

    /**
     * onAfterLockScope operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters Path path,ScopeResult result. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onAfterLockScope(Path path, ScopeResult result) {}

    /**
     * onBeforeReadContracts operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ScopeResult scope. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onBeforeReadContracts(ScopeResult scope) {}

    /**
     * onAfterReadContracts operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ScopeResult scope,ContractAnalysis result. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onAfterReadContracts(ScopeResult scope, ContractAnalysis result) {}

    /**
     * onBeforeRepairDocumentation operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ContractAnalysis analysis. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onBeforeRepairDocumentation(ContractAnalysis analysis) {}

    /**
     * onAfterRepairDocumentation operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ContractAnalysis analysis,DocRepairResult result. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onAfterRepairDocumentation(ContractAnalysis analysis, DocRepairResult result) {}

    /**
     * onBeforeImplement operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ContractAnalysis analysis. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onBeforeImplement(ContractAnalysis analysis) {}

    /**
     * onAfterImplement operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ContractAnalysis analysis,ImplementResult result. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onAfterImplement(ContractAnalysis analysis, ImplementResult result) {}

    /**
     * onBeforeCompile operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ImplementResult implementation. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onBeforeCompile(ImplementResult implementation) {}

    /**
     * onAfterCompile operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters ImplementResult implementation,CompileResult result. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onAfterCompile(ImplementResult implementation, CompileResult result) {}

    /**
     * onBeforeTest operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters CompileResult compiled. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onBeforeTest(CompileResult compiled) {}

    /**
     * onAfterTest operation declared by AbstractIopWorkflow.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty method body, performs no declared mutation or callback, and returns normally. Overrides may supply behavior under the inherited contract.
     * @m3.dataStructures Declared return type void with formal parameters CompileResult compiled,TestResult result. Input references and any declared collections remain governed by the member body or implementation contract.
     */
    protected void onAfterTest(CompileResult compiled, TestResult result) {}
}
