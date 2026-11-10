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

import java.nio.file.Path;
import java.util.List;

/**
 * IOP Workflow - Locked coding workflow specification.
 *
 * <h2>FIRST-HAND INSTRUCTION - CANNOT BE OVERRIDDEN</h2>
 *
 * <pre>
 * IOP: CodeAsInstructionStateImplementationMemory
 *
 * PARADIGM STACK (Order Matters):
 * COP → AOP → OOP → FOP → IOP → AnOP
 * absorbing GoF + EIP + Microservice + DAGFlow patterns
 * with compile-time authority
 * </pre>
 *
 * <h2>The Fundamental Principle</h2> <blockquote> "When code is the instruction
 * and code is the document, AI does not need memory - it needs literacy."
 * </blockquote>
 *
 * <h2>Workflow Phases</h2>
 *
 * <pre>
 * PHASE 0: SCOPE_LOCK - Bounded scope (file/package/module)
 * PHASE 1: READ_ONLY - Read contracts, no code changes
 * PHASE 2: DOC_REPAIR - Fix JavaDoc only (optional)
 * PHASE 3: IMPLEMENT - Generate/update ONE implementation
 * PHASE 4: COMPILE - Fix compiler errors only
 * PHASE 5: TEST - Tests outrank AI
 * </pre>
 *
 * <h2>Authority Hierarchy</h2>
 *
 * <pre>
 * JavaDoc / Contracts (highest)
 * ↓
 * Compiler
 * ↓
 * Tests
 * ↓
 * Folder boundaries
 * ↓
 * AI output (lowest)
 * </pre>
 *
 * @since 1.1.0 - IOP Integration
 * @m3.pattern synexia:bop
 * @m3.algorithm Groups declared fields LOCKED, VERSION, ONE_SENTENCE, ONE_RULE, FINAL_CLOSURE, PARADIGM_STACK, PATTERNS_ABSORBED and member operations lockScope, readContracts, repairDocumentation, implement, compile, test, executeWorkflow. Concrete bodies and inherited behavior remain owned by their member and parent contracts.
 * @m3.dataStructures Declared interface owner with boolean LOCKED, String VERSION, String ONE_SENTENCE, String ONE_RULE, String FINAL_CLOSURE, String PARADIGM_STACK, String PATTERNS_ABSORBED. Nested types and inherited members retain their own declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IopWorkflow.java}
 * blob {@code ed0a7ee9624e30a81ec341bfd5dbfa53ad02a37b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IopWorkflow {

    // --- LOCK CONSTANTS ---

    /** Lock status - permanently locked.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes LOCKED by evaluating the checked-in expression true. This interface field is implicitly public static final, and its value is shared by the declaring interface.
     * @m3.dataStructures One boolean declared field named LOCKED.
     */
    boolean LOCKED = true;

    /** Lock version.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes VERSION by evaluating the checked-in expression "1.0.0-FINAL". This interface field is implicitly public static final, and its stored reference is shared by the declaring interface.
     * @m3.dataStructures One String declared field named VERSION.
     */
    String VERSION = "1.0.0-FINAL";

    /** The one sentence rule.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes ONE_SENTENCE by evaluating the checked-in expression "If it\'s not in the interface or JavaDoc, it does not exist.". This interface field is implicitly public static final, and its stored reference is shared by the declaring interface.
     * @m3.dataStructures One String declared field named ONE_SENTENCE.
     */
    String ONE_SENTENCE = "If it's not in the interface or JavaDoc, it does not exist.";

    /** The one rule that matters.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes ONE_RULE by evaluating the checked-in expression "If the AI cannot implement the code by reading the JavaDoc, the JavaDoc is wrong - not the AI.". This interface field is implicitly public static final, and its stored reference is shared by the declaring interface.
     * @m3.dataStructures One String declared field named ONE_RULE.
     */
    String ONE_RULE =
            "If the AI cannot implement the code by reading the JavaDoc, " + "the JavaDoc is wrong - not the AI.";

    /** Final closure.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes FINAL_CLOSURE by evaluating the checked-in expression "You are not here to be clever. You are here to make contracts real.". This interface field is implicitly public static final, and its stored reference is shared by the declaring interface.
     * @m3.dataStructures One String declared field named FINAL_CLOSURE.
     */
    String FINAL_CLOSURE = "You are not here to be clever. You are here to make contracts real.";

    /** Paradigm stack.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes PARADIGM_STACK by evaluating the checked-in expression "COP → AOP → OOP → FOP → IOP → AnOP". This interface field is implicitly public static final, and its stored reference is shared by the declaring interface.
     * @m3.dataStructures One String declared field named PARADIGM_STACK.
     */
    String PARADIGM_STACK = "COP → AOP → OOP → FOP → IOP → AnOP";

    /** PatternKind families absorbed.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes PATTERNS_ABSORBED by evaluating the checked-in expression "GoF + EIP + Microservice + DAGFlow". This interface field is implicitly public static final, and its stored reference is shared by the declaring interface.
     * @m3.dataStructures One String declared field named PATTERNS_ABSORBED.
     */
    String PATTERNS_ABSORBED = "GoF + EIP + Microservice + DAGFlow";

    // --- WORKFLOW PHASES ---

    /** Workflow phases - must be executed in order.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares enum singleton values in source order with declared fields SCOPE_LOCK, READ_ONLY, DOC_REPAIR, IMPLEMENT, COMPILE, TEST and member operations none.
     * @m3.dataStructures Declared enum owner with Phase SCOPE_LOCK, Phase READ_ONLY, Phase DOC_REPAIR, Phase IMPLEMENT, Phase COMPILE, Phase TEST. Nested types and inherited members retain their own declarations.
     */
    enum Phase {
        /**
         * Phase 0: Scope Lock. Identify bounded scope (file/package/module). If scope
         * larger → REFUSE.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes SCOPE_LOCK by evaluating the checked-in expression new Phase(). The field is shared by the declared owner.
         * @m3.dataStructures One Phase declared field named SCOPE_LOCK.
         */
        SCOPE_LOCK,

        /**
         * Phase 1: Read-Only. Read contracts, JavaDoc, annotations. No code changes
         * yet. If unclear → STOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes READ_ONLY by evaluating the checked-in expression new Phase(). The field is shared by the declared owner.
         * @m3.dataStructures One Phase declared field named READ_ONLY.
         */
        READ_ONLY,

        /**
         * Phase 2: Documentation Repair. Fix/improve JavaDoc only. No implementation
         * changes. This step alone is SUCCESS.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes DOC_REPAIR by evaluating the checked-in expression new Phase(). The field is shared by the declared owner.
         * @m3.dataStructures One Phase declared field named DOC_REPAIR.
         */
        DOC_REPAIR,

        /**
         * Phase 3: Implementation. Generate/update ONE implementation. Stay inside
         * declared package. Boring, explicit Java.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes IMPLEMENT by evaluating the checked-in expression new Phase(). The field is shared by the declared owner.
         * @m3.dataStructures One Phase declared field named IMPLEMENT.
         */
        IMPLEMENT,

        /**
         * Phase 4: Compile Feedback. Compiler errors are HARD AUTHORITY. Fix only what
         * compiler indicates. Never expand scope.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes COMPILE by evaluating the checked-in expression new Phase(). The field is shared by the declared owner.
         * @m3.dataStructures One Phase declared field named COMPILE.
         */
        COMPILE,

        /** Phase 5: Test Feedback. Tests outrank AI. Fix BEHAVIOR, not tests.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes TEST by evaluating the checked-in expression new Phase(). The field is shared by the declared owner.
         * @m3.dataStructures One Phase declared field named TEST.
         */
        TEST
    }

    /** Cognitive layers for AI.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares enum singleton values in source order with declared fields IOP_AUTHORITY, ANOP_EXPRESSION, STRUCTURAL_BASEMENT and member operations none.
     * @m3.dataStructures Declared enum owner with CognitiveLayer IOP_AUTHORITY, CognitiveLayer ANOP_EXPRESSION, CognitiveLayer STRUCTURAL_BASEMENT. Nested types and inherited members retain their own declarations.
     */
    enum CognitiveLayer {
        /**
         * Layer 1: IOP - Authority Layer. Interfaces, public records, JavaDoc, method
         * signatures. Treated as: law, spec, instruction, prompt. If unclear → AI MUST
         * STOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes IOP_AUTHORITY by evaluating the checked-in expression new CognitiveLayer(). The field is shared by the declared owner.
         * @m3.dataStructures One CognitiveLayer declared field named IOP_AUTHORITY.
         */
        IOP_AUTHORITY,

        /**
         * Layer 2: AnOP - Expression Layer. Annotations are adjectives, not verbs.
         * Compile-time only. No runtime effects.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes ANOP_EXPRESSION by evaluating the checked-in expression new CognitiveLayer(). The field is shared by the declared owner.
         * @m3.dataStructures One CognitiveLayer declared field named ANOP_EXPRESSION.
         */
        ANOP_EXPRESSION,

        /**
         * Layer 3: Structural Basement. COP, AOP, OOP, FOP, GoF, EIP, Micro patterns.
         * Use internally, never surface upward.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes STRUCTURAL_BASEMENT by evaluating the checked-in expression new CognitiveLayer(). The field is shared by the declared owner.
         * @m3.dataStructures One CognitiveLayer declared field named STRUCTURAL_BASEMENT.
         */
        STRUCTURAL_BASEMENT
    }

    /** Authority hierarchy levels.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares enum singleton values in source order with declared fields JAVADOC_CONTRACTS, COMPILER, TESTS, FOLDER_BOUNDARIES, AI_OUTPUT, rank and member operations &lt;init&gt;, rank, outranks.
     * @m3.dataStructures Declared enum owner with Authority JAVADOC_CONTRACTS, Authority COMPILER, Authority TESTS, Authority FOLDER_BOUNDARIES, Authority AI_OUTPUT, int rank. Nested types and inherited members retain their own declarations.
     */
    enum Authority {

        /** Highest - JavaDoc and contracts.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes JAVADOC_CONTRACTS by evaluating the checked-in expression new Authority(1). The field is shared by the declared owner.
         * @m3.dataStructures One Authority declared field named JAVADOC_CONTRACTS.
         */
        JAVADOC_CONTRACTS(1),
        /** Second - Compiler.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes COMPILER by evaluating the checked-in expression new Authority(2). The field is shared by the declared owner.
         * @m3.dataStructures One Authority declared field named COMPILER.
         */
        COMPILER(2),
        /** Third - Tests.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes TESTS by evaluating the checked-in expression new Authority(3). The field is shared by the declared owner.
         * @m3.dataStructures One Authority declared field named TESTS.
         */
        TESTS(3),
        /** Fourth - Folder boundaries.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes FOLDER_BOUNDARIES by evaluating the checked-in expression new Authority(4). The field is shared by the declared owner.
         * @m3.dataStructures One Authority declared field named FOLDER_BOUNDARIES.
         */
        FOLDER_BOUNDARIES(4),
        /** Lowest - AI output.
         * @m3.pattern synexia:iop
         * @m3.algorithm Initializes AI_OUTPUT by evaluating the checked-in expression new Authority(5). The field is shared by the declared owner.
         * @m3.dataStructures One Authority declared field named AI_OUTPUT.
         */
        AI_OUTPUT(5);

        /**
         * Stored rank member of IopWorkflow.Authority.
         * @m3.pattern synexia:iop
         * @m3.algorithm Has no inline initializer. A declaring constructor may assign it, otherwise Java default initialization applies. This declaration performs no validation or copying.
         * @m3.dataStructures One int declared field named rank.
         */
        private final int rank;

        /**
         * Initializes an instance of IopWorkflow.Authority.
         * @m3.pattern synexia:bop
         * @m3.algorithm Executes the declared statement sequence { this.rank = rank; }. Java evaluation order, called-operation failures and the declared catch clauses remain those shown in this sequence.
         * @m3.dataStructures Declared constructor with formal parameters int rank. Input references and declared collections follow the member body or implementation contract.
         */
        Authority(int rank) {
            this.rank = rank;
        }

        /**
         * Get authority rank.
         *
         * @return rank (lower = higher authority)
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the stored rank value directly without copying or normalization.
         * @m3.dataStructures Declared return type int with no formal parameters. Input references and declared collections follow the member body or implementation contract.
         */
        public int rank() {
            return rank;
        }

        /**
         * Check if this authority outranks another.
         *
         * @param other
         *            the other authority
         * @return true if this outranks other
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns whether this stored rank is numerically less than other.rank. Null other fails on dereference and equality is false.
         * @m3.dataStructures Declared return type boolean with formal parameters Authority other. Input references and declared collections follow the member body or implementation contract.
         */
        public boolean outranks(Authority other) {
            return this.rank < other.rank;
        }
    }

    // --- WORKFLOW METHODS ---

    /**
     * Execute Phase 0: Scope Lock.
     *
     * @param path
     *            target path (file, package dir, or module)
     * @return scope result with bounded context
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body or annotation default, and its concrete implementation supplies the algorithm.; review=flow-context-declarations:local-review/IopWorkflow/5576
     * @m3.dataStructures Declared return type ScopeResult with formal parameters Path path. Input references and declared collections follow the member body or implementation contract.
     */
    ScopeResult lockScope(Path path);

    /**
     * Execute Phase 1: Read-Only analysis.
     *
     * @param scope
     *            the locked scope
     * @return contract analysis result
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body or annotation default, and its concrete implementation supplies the algorithm.; review=flow-context-declarations:local-review/IopWorkflow/5777
     * @m3.dataStructures Declared return type ContractAnalysis with formal parameters ScopeResult scope. Input references and declared collections follow the member body or implementation contract.
     */
    ContractAnalysis readContracts(ScopeResult scope);

    /**
     * Execute Phase 2: Documentation Repair.
     *
     * @param analysis
     *            the contract analysis
     * @return documentation fixes
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body or annotation default, and its concrete implementation supplies the algorithm.; review=flow-context-declarations:local-review/IopWorkflow/6000
     * @m3.dataStructures Declared return type DocRepairResult with formal parameters ContractAnalysis analysis. Input references and declared collections follow the member body or implementation contract.
     */
    DocRepairResult repairDocumentation(ContractAnalysis analysis);

    /**
     * Execute Phase 3: Implementation.
     *
     * @param analysis
     *            the contract analysis
     * @return implementation result
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body or annotation default, and its concrete implementation supplies the algorithm.; review=flow-context-declarations:local-review/IopWorkflow/6232
     * @m3.dataStructures Declared return type ImplementResult with formal parameters ContractAnalysis analysis. Input references and declared collections follow the member body or implementation contract.
     */
    ImplementResult implement(ContractAnalysis analysis);

    /**
     * Execute Phase 4: Compile Feedback.
     *
     * @param implementation
     *            the implementation
     * @return compile result
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body or annotation default, and its concrete implementation supplies the algorithm.; review=flow-context-declarations:local-review/IopWorkflow/6452
     * @m3.dataStructures Declared return type CompileResult with formal parameters ImplementResult implementation. Input references and declared collections follow the member body or implementation contract.
     */
    CompileResult compile(ImplementResult implementation);

    /**
     * Execute Phase 5: Test Feedback.
     *
     * @param compiled
     *            the compiled result
     * @return test result
     * @m3.pattern synexia:bop
     * @m3.applicability algorithm=not-applicable; reason=This method signature declares no executable body or annotation default, and its concrete implementation supplies the algorithm.; review=flow-context-declarations:local-review/IopWorkflow/6662
     * @m3.dataStructures Declared return type TestResult with formal parameters CompileResult compiled. Input references and declared collections follow the member body or implementation contract.
     */
    TestResult test(CompileResult compiled);

    /**
     * Execute full workflow.
     *
     * @param path
     *            target path
     * @return workflow result
     * @m3.pattern synexia:bop
     * @m3.algorithm Calls lockScope and stops with scopeFailed when its result is invalid. Calls readContracts and stops with contractsUnclear when incomplete. Otherwise calls repairDocumentation, implement, compile and test in that order and passes all results to WorkflowResult.success. Implementation and compilation success flags and test failure counts do not stop this method. Null scope or analysis fails on dereference and exceptions propagate without recovery.
     * @m3.dataStructures Declared return type WorkflowResult with formal parameters Path path. Input references and declared collections follow the member body or implementation contract.
     */
    default WorkflowResult executeWorkflow(Path path) {
        ScopeResult scope = lockScope(path);
        if (!scope.isValid()) {
            return WorkflowResult.scopeFailed(scope);
        }

        ContractAnalysis analysis = readContracts(scope);
        if (!analysis.isComplete()) {
            return WorkflowResult.contractsUnclear(analysis);
        }

        DocRepairResult docRepair = repairDocumentation(analysis);
        ImplementResult impl = implement(analysis);
        CompileResult compile = compile(impl);
        TestResult tests = test(compile);

        return WorkflowResult.success(scope, analysis, docRepair, impl, compile, tests);
    }

    // --- RESULT TYPES ---

    /**
     * Scope lock result.
     *
     * @param path
     *            the path
     * @param scopeType
     *            file, package, or module
     * @param isValid
     *            whether scope is valid
     * @param reason
     *            failure reason if invalid
     * @param files
     *            files in scope
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence path, scopeType, isValid, reason, files. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with Path path, ScopeType scopeType, boolean isValid, String reason, List of Path files. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:path} pattern=synexia:iop; algorithm=Stores the caller-supplied path component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One Path record component field named path.; description=Stored path member of IopWorkflow.ScopeResult.
     * @m3.member {@code FIELD:scopeType} pattern=synexia:iop; algorithm=Stores the caller-supplied scopeType component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One ScopeType record component field named scopeType.; description=Stored scopeType member of IopWorkflow.ScopeResult.
     * @m3.member {@code FIELD:isValid} pattern=synexia:iop; algorithm=Stores the caller-supplied isValid component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One boolean record component field named isValid.; description=Stored isValid member of IopWorkflow.ScopeResult.
     * @m3.member {@code FIELD:reason} pattern=synexia:iop; algorithm=Stores the caller-supplied reason component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named reason.; description=Stored reason member of IopWorkflow.ScopeResult.
     * @m3.member {@code FIELD:files} pattern=synexia:iop; algorithm=Stores the caller-supplied files component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of Path record component field named files.; description=Stored files member of IopWorkflow.ScopeResult.
     */
    record ScopeResult(Path path, ScopeType scopeType, boolean isValid, String reason, List<Path> files) {

        /** Scope types.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares enum singleton values in source order with declared fields FILE, PACKAGE, MODULE and member operations none.
         * @m3.dataStructures Declared enum owner with ScopeType FILE, ScopeType PACKAGE, ScopeType MODULE. Nested types and inherited members retain their own declarations.
         */
        public enum ScopeType {
            /**
             * Stored FILE member of IopWorkflow.ScopeResult.ScopeType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes FILE by evaluating the checked-in expression new ScopeType(). The field is shared by the declared owner.
             * @m3.dataStructures One ScopeType declared field named FILE.
             */
            FILE,
            /**
             * Stored PACKAGE member of IopWorkflow.ScopeResult.ScopeType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes PACKAGE by evaluating the checked-in expression new ScopeType(). The field is shared by the declared owner.
             * @m3.dataStructures One ScopeType declared field named PACKAGE.
             */
            PACKAGE,
            /**
             * Stored MODULE member of IopWorkflow.ScopeResult.ScopeType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes MODULE by evaluating the checked-in expression new ScopeType(). The field is shared by the declared owner.
             * @m3.dataStructures One ScopeType declared field named MODULE.
             */
            MODULE
        }

        /** Create valid scope.
         * @m3.pattern synexia:bop
         * @m3.algorithm Constructs a valid ScopeResult with the caller path, scope type and files reference, null reason and isValid true. The list is retained without copying or null validation.
         * @m3.dataStructures Declared return type ScopeResult with formal parameters Path path,ScopeType type,List of Path files. Input references and declared collections follow the member body or implementation contract.
         */
        public static ScopeResult valid(Path path, ScopeType type, List<Path> files) {
            return new ScopeResult(path, type, true, null, files);
        }

        /** Create invalid scope.
         * @m3.pattern synexia:bop
         * @m3.algorithm Constructs an invalid ScopeResult retaining path and reason, using null scope type, isValid false and an immutable empty files list.
         * @m3.dataStructures Declared return type ScopeResult with formal parameters Path path,String reason. Input references and declared collections follow the member body or implementation contract.
         */
        public static ScopeResult invalid(Path path, String reason) {
            return new ScopeResult(path, null, false, reason, List.of());
        }
    }

    /**
     * Contract analysis result.
     *
     * @param interfaces
     *            found interfaces
     * @param javadocCoverage
     *            percentage of methods with JavaDoc
     * @param issues
     *            found issues
     * @param isComplete
     *            whether analysis is complete
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence interfaces, javadocCoverage, issues, isComplete. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with List of InterfaceContract interfaces, double javadocCoverage, List of ContractIssue issues, boolean isComplete. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:interfaces} pattern=synexia:iop; algorithm=Stores the caller-supplied interfaces component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of InterfaceContract record component field named interfaces.; description=Stored interfaces member of IopWorkflow.ContractAnalysis.
     * @m3.member {@code FIELD:javadocCoverage} pattern=synexia:iop; algorithm=Stores the caller-supplied javadocCoverage component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One double record component field named javadocCoverage.; description=Stored javadocCoverage member of IopWorkflow.ContractAnalysis.
     * @m3.member {@code FIELD:issues} pattern=synexia:iop; algorithm=Stores the caller-supplied issues component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of ContractIssue record component field named issues.; description=Stored issues member of IopWorkflow.ContractAnalysis.
     * @m3.member {@code FIELD:isComplete} pattern=synexia:iop; algorithm=Stores the caller-supplied isComplete component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One boolean record component field named isComplete.; description=Stored isComplete member of IopWorkflow.ContractAnalysis.
     */
    record ContractAnalysis(
            List<InterfaceContract> interfaces,
            double javadocCoverage,
            List<ContractIssue> issues,
            boolean isComplete) {}

    /**
     * Interface contract.
     *
     * @param name
     *            interface name
     * @param packageName
     *            package
     * @param methods
     *            method signatures
     * @param hasJavadoc
     *            whether interface has JavaDoc
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence name, packageName, methods, hasJavadoc. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with String name, String packageName, List of MethodSignature methods, boolean hasJavadoc. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm=Stores the caller-supplied name component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named name.; description=Stored name member of IopWorkflow.InterfaceContract.
     * @m3.member {@code FIELD:packageName} pattern=synexia:iop; algorithm=Stores the caller-supplied packageName component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named packageName.; description=Stored packageName member of IopWorkflow.InterfaceContract.
     * @m3.member {@code FIELD:methods} pattern=synexia:iop; algorithm=Stores the caller-supplied methods component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of MethodSignature record component field named methods.; description=Stored methods member of IopWorkflow.InterfaceContract.
     * @m3.member {@code FIELD:hasJavadoc} pattern=synexia:iop; algorithm=Stores the caller-supplied hasJavadoc component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One boolean record component field named hasJavadoc.; description=Stored hasJavadoc member of IopWorkflow.InterfaceContract.
     */
    record InterfaceContract(String name, String packageName, List<MethodSignature> methods, boolean hasJavadoc) {}

    /**
     * Method signature.
     *
     * @param name
     *            method name
     * @param returnType
     *            return type
     * @param parameters
     *            parameters
     * @param hasJavadoc
     *            whether method has JavaDoc
     * @param javadoc
     *            the JavaDoc if present
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence name, returnType, parameters, hasJavadoc, javadoc. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with String name, String returnType, List of String parameters, boolean hasJavadoc, String javadoc. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm=Stores the caller-supplied name component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named name.; description=Stored name member of IopWorkflow.MethodSignature.
     * @m3.member {@code FIELD:returnType} pattern=synexia:iop; algorithm=Stores the caller-supplied returnType component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named returnType.; description=Stored returnType member of IopWorkflow.MethodSignature.
     * @m3.member {@code FIELD:parameters} pattern=synexia:iop; algorithm=Stores the caller-supplied parameters component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of String record component field named parameters.; description=Stored parameters member of IopWorkflow.MethodSignature.
     * @m3.member {@code FIELD:hasJavadoc} pattern=synexia:iop; algorithm=Stores the caller-supplied hasJavadoc component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One boolean record component field named hasJavadoc.; description=Stored hasJavadoc member of IopWorkflow.MethodSignature.
     * @m3.member {@code FIELD:javadoc} pattern=synexia:iop; algorithm=Stores the caller-supplied javadoc component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named javadoc.; description=Stored javadoc member of IopWorkflow.MethodSignature.
     */
    record MethodSignature(
            String name, String returnType, List<String> parameters, boolean hasJavadoc, String javadoc) {}

    /**
     * Contract issue.
     *
     * @param type
     *            issue type
     * @param location
     *            where the issue is
     * @param message
     *            description
     * @param severity
     *            severity level
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence type, location, message, severity. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with IssueType type, String location, String message, Severity severity. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm=Stores the caller-supplied type component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One IssueType record component field named type.; description=Stored type member of IopWorkflow.ContractIssue.
     * @m3.member {@code FIELD:location} pattern=synexia:iop; algorithm=Stores the caller-supplied location component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named location.; description=Stored location member of IopWorkflow.ContractIssue.
     * @m3.member {@code FIELD:message} pattern=synexia:iop; algorithm=Stores the caller-supplied message component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named message.; description=Stored message member of IopWorkflow.ContractIssue.
     * @m3.member {@code FIELD:severity} pattern=synexia:iop; algorithm=Stores the caller-supplied severity component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One Severity record component field named severity.; description=Stored severity member of IopWorkflow.ContractIssue.
     */
    record ContractIssue(IssueType type, String location, String message, Severity severity) {

        /** Issue types.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares enum singleton values in source order with declared fields MISSING_JAVADOC, VAGUE_JAVADOC, MISSING_CONTRACT, AMBIGUOUS_RETURN, MISSING_PARAM_DOC, UNCLEAR_EXCEPTION and member operations none.
         * @m3.dataStructures Declared enum owner with IssueType MISSING_JAVADOC, IssueType VAGUE_JAVADOC, IssueType MISSING_CONTRACT, IssueType AMBIGUOUS_RETURN, IssueType MISSING_PARAM_DOC, IssueType UNCLEAR_EXCEPTION. Nested types and inherited members retain their own declarations.
         */
        public enum IssueType {
            /**
             * Stored MISSING_JAVADOC member of IopWorkflow.ContractIssue.IssueType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes MISSING_JAVADOC by evaluating the checked-in expression new IssueType(). The field is shared by the declared owner.
             * @m3.dataStructures One IssueType declared field named MISSING_JAVADOC.
             */
            MISSING_JAVADOC,
            /**
             * Stored VAGUE_JAVADOC member of IopWorkflow.ContractIssue.IssueType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes VAGUE_JAVADOC by evaluating the checked-in expression new IssueType(). The field is shared by the declared owner.
             * @m3.dataStructures One IssueType declared field named VAGUE_JAVADOC.
             */
            VAGUE_JAVADOC,
            /**
             * Stored MISSING_CONTRACT member of IopWorkflow.ContractIssue.IssueType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes MISSING_CONTRACT by evaluating the checked-in expression new IssueType(). The field is shared by the declared owner.
             * @m3.dataStructures One IssueType declared field named MISSING_CONTRACT.
             */
            MISSING_CONTRACT,
            /**
             * Stored AMBIGUOUS_RETURN member of IopWorkflow.ContractIssue.IssueType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes AMBIGUOUS_RETURN by evaluating the checked-in expression new IssueType(). The field is shared by the declared owner.
             * @m3.dataStructures One IssueType declared field named AMBIGUOUS_RETURN.
             */
            AMBIGUOUS_RETURN,
            /**
             * Stored MISSING_PARAM_DOC member of IopWorkflow.ContractIssue.IssueType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes MISSING_PARAM_DOC by evaluating the checked-in expression new IssueType(). The field is shared by the declared owner.
             * @m3.dataStructures One IssueType declared field named MISSING_PARAM_DOC.
             */
            MISSING_PARAM_DOC,
            /**
             * Stored UNCLEAR_EXCEPTION member of IopWorkflow.ContractIssue.IssueType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes UNCLEAR_EXCEPTION by evaluating the checked-in expression new IssueType(). The field is shared by the declared owner.
             * @m3.dataStructures One IssueType declared field named UNCLEAR_EXCEPTION.
             */
            UNCLEAR_EXCEPTION
        }

        /** Severity levels.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares enum singleton values in source order with declared fields ERROR, WARNING, INFO and member operations none.
         * @m3.dataStructures Declared enum owner with Severity ERROR, Severity WARNING, Severity INFO. Nested types and inherited members retain their own declarations.
         */
        public enum Severity {
            /**
             * Stored ERROR member of IopWorkflow.ContractIssue.Severity.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes ERROR by evaluating the checked-in expression new Severity(). The field is shared by the declared owner.
             * @m3.dataStructures One Severity declared field named ERROR.
             */
            ERROR,
            /**
             * Stored WARNING member of IopWorkflow.ContractIssue.Severity.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes WARNING by evaluating the checked-in expression new Severity(). The field is shared by the declared owner.
             * @m3.dataStructures One Severity declared field named WARNING.
             */
            WARNING,
            /**
             * Stored INFO member of IopWorkflow.ContractIssue.Severity.
             * @m3.pattern synexia:iop
             * @m3.algorithm Initializes INFO by evaluating the checked-in expression new Severity(). The field is shared by the declared owner.
             * @m3.dataStructures One Severity declared field named INFO.
             */
            INFO
        }
    }

    /**
     * Documentation repair result.
     *
     * @param fixedFiles
     *            files with fixed JavaDoc
     * @param improvementCount
     *            number of improvements
     * @param beforeCoverage
     *            coverage before
     * @param afterCoverage
     *            coverage after
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence fixedFiles, improvementCount, beforeCoverage, afterCoverage. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with List of Path fixedFiles, int improvementCount, double beforeCoverage, double afterCoverage. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:fixedFiles} pattern=synexia:iop; algorithm=Stores the caller-supplied fixedFiles component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of Path record component field named fixedFiles.; description=Stored fixedFiles member of IopWorkflow.DocRepairResult.
     * @m3.member {@code FIELD:improvementCount} pattern=synexia:iop; algorithm=Stores the caller-supplied improvementCount component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One int record component field named improvementCount.; description=Stored improvementCount member of IopWorkflow.DocRepairResult.
     * @m3.member {@code FIELD:beforeCoverage} pattern=synexia:iop; algorithm=Stores the caller-supplied beforeCoverage component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One double record component field named beforeCoverage.; description=Stored beforeCoverage member of IopWorkflow.DocRepairResult.
     * @m3.member {@code FIELD:afterCoverage} pattern=synexia:iop; algorithm=Stores the caller-supplied afterCoverage component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One double record component field named afterCoverage.; description=Stored afterCoverage member of IopWorkflow.DocRepairResult.
     */
    record DocRepairResult(List<Path> fixedFiles, int improvementCount, double beforeCoverage, double afterCoverage) {}

    /**
     * Implementation result.
     *
     * @param generatedFiles
     *            generated files
     * @param implementedContracts
     *            contracts implemented
     * @param success
     *            whether successful
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence generatedFiles, implementedContracts, success. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with List of Path generatedFiles, List of String implementedContracts, boolean success. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:generatedFiles} pattern=synexia:iop; algorithm=Stores the caller-supplied generatedFiles component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of Path record component field named generatedFiles.; description=Stored generatedFiles member of IopWorkflow.ImplementResult.
     * @m3.member {@code FIELD:implementedContracts} pattern=synexia:iop; algorithm=Stores the caller-supplied implementedContracts component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of String record component field named implementedContracts.; description=Stored implementedContracts member of IopWorkflow.ImplementResult.
     * @m3.member {@code FIELD:success} pattern=synexia:iop; algorithm=Stores the caller-supplied success component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One boolean record component field named success.; description=Stored success member of IopWorkflow.ImplementResult.
     */
    record ImplementResult(List<Path> generatedFiles, List<String> implementedContracts, boolean success) {}

    /**
     * Compile result.
     *
     * @param success
     *            whether compilation succeeded
     * @param errors
     *            compiler errors
     * @param fixedErrors
     *            errors that were fixed
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence success, errors, fixedErrors. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with boolean success, List of String errors, List of String fixedErrors. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:success} pattern=synexia:iop; algorithm=Stores the caller-supplied success component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One boolean record component field named success.; description=Stored success member of IopWorkflow.CompileResult.
     * @m3.member {@code FIELD:errors} pattern=synexia:iop; algorithm=Stores the caller-supplied errors component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of String record component field named errors.; description=Stored errors member of IopWorkflow.CompileResult.
     * @m3.member {@code FIELD:fixedErrors} pattern=synexia:iop; algorithm=Stores the caller-supplied fixedErrors component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One List of String record component field named fixedErrors.; description=Stored fixedErrors member of IopWorkflow.CompileResult.
     */
    record CompileResult(boolean success, List<String> errors, List<String> fixedErrors) {}

    /**
     * Test result.
     *
     * @param passed
     *            tests passed
     * @param failed
     *            tests failed
     * @param coverage
     *            code coverage
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence passed, failed, coverage. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with int passed, int failed, double coverage. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:passed} pattern=synexia:iop; algorithm=Stores the caller-supplied passed component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One int record component field named passed.; description=Stored passed member of IopWorkflow.TestResult.
     * @m3.member {@code FIELD:failed} pattern=synexia:iop; algorithm=Stores the caller-supplied failed component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One int record component field named failed.; description=Stored failed member of IopWorkflow.TestResult.
     * @m3.member {@code FIELD:coverage} pattern=synexia:iop; algorithm=Stores the caller-supplied coverage component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One double record component field named coverage.; description=Stored coverage member of IopWorkflow.TestResult.
     */
    record TestResult(int passed, int failed, double coverage) {}

    /**
     * Full workflow result.
     *
     * @param phase
     *            final phase reached
     * @param success
     *            overall success
     * @param scope
     *            scope result
     * @param contracts
     *            contract analysis
     * @param docRepair
     *            doc repair result
     * @param implementation
     *            implementation result
     * @param compile
     *            compile result
     * @param test
     *            test result
     * @param message
     *            summary message
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the record component sequence phase, success, scope, contracts, docRepair, implementation, compile, test, message. The generated canonical constructor stores component values without declared validation or defensive copying, and generated accessors return those stored values.
     * @m3.dataStructures Declared record owner with Phase phase, boolean success, ScopeResult scope, ContractAnalysis contracts, DocRepairResult docRepair, ImplementResult implementation, CompileResult compile, TestResult test, String message. Nested types and inherited members retain their own declarations.
     * @m3.member {@code FIELD:phase} pattern=synexia:iop; algorithm=Stores the caller-supplied phase component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One Phase record component field named phase.; description=Stored phase member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:success} pattern=synexia:iop; algorithm=Constructs a result at TEST with success true, retaining all six supplied result references and the fixed completion message. It does not validate those results or inspect their own success indicators.; data.structures=One boolean record component field named success.; description=Stored success member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:scope} pattern=synexia:iop; algorithm=Stores the caller-supplied scope component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One ScopeResult record component field named scope.; description=Stored scope member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:contracts} pattern=synexia:iop; algorithm=Stores the caller-supplied contracts component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One ContractAnalysis record component field named contracts.; description=Stored contracts member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:docRepair} pattern=synexia:iop; algorithm=Stores the caller-supplied docRepair component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One DocRepairResult record component field named docRepair.; description=Stored docRepair member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:implementation} pattern=synexia:iop; algorithm=Stores the caller-supplied implementation component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One ImplementResult record component field named implementation.; description=Stored implementation member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:compile} pattern=synexia:iop; algorithm=Stores the caller-supplied compile component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One CompileResult record component field named compile.; description=Stored compile member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:test} pattern=synexia:iop; algorithm=Stores the caller-supplied test component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One TestResult record component field named test.; description=Stored test member of IopWorkflow.WorkflowResult.
     * @m3.member {@code FIELD:message} pattern=synexia:iop; algorithm=Stores the caller-supplied message component value in the generated final record field. Reference values remain aliased because this record declares no component copying.; data.structures=One String record component field named message.; description=Stored message member of IopWorkflow.WorkflowResult.
     */
    record WorkflowResult(
            Phase phase,
            boolean success,
            ScopeResult scope,
            ContractAnalysis contracts,
            DocRepairResult docRepair,
            ImplementResult implementation,
            CompileResult compile,
            TestResult test,
            String message) {

        /** Create scope failed result.
         * @m3.pattern synexia:bop
         * @m3.algorithm Constructs a failure at SCOPE_LOCK retaining the supplied scope reference, null later result components and a message concatenated from scope.reason. Null scope fails when its reason is read.
         * @m3.dataStructures Declared return type WorkflowResult with formal parameters ScopeResult scope. Input references and declared collections follow the member body or implementation contract.
         */
        public static WorkflowResult scopeFailed(ScopeResult scope) {
            return new WorkflowResult(
                    Phase.SCOPE_LOCK,
                    false,
                    scope,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Scope lock failed: " + scope.reason());
        }

        /** Create contracts unclear result.
         * @m3.pattern synexia:bop
         * @m3.algorithm Constructs a failure at READ_ONLY retaining the analysis reference, null scope and later result components and the fixed contracts-unclear message. No analysis validation or copying occurs.
         * @m3.dataStructures Declared return type WorkflowResult with formal parameters ContractAnalysis analysis. Input references and declared collections follow the member body or implementation contract.
         */
        public static WorkflowResult contractsUnclear(ContractAnalysis analysis) {
            return new WorkflowResult(
                    Phase.READ_ONLY, false, null, analysis, null, null, null, null, "Contracts unclear - AI must stop");
        }

        /** Create success result.
         * @m3.pattern synexia:bop
         * @m3.algorithm Constructs a result at TEST with success true, retaining all six supplied result references and the fixed completion message. It does not validate those results or inspect their own success indicators.
         * @m3.dataStructures Declared return type WorkflowResult with formal parameters ScopeResult scope,ContractAnalysis contracts,DocRepairResult docRepair,ImplementResult impl,CompileResult compile,TestResult test. Input references and declared collections follow the member body or implementation contract.
         */
        public static WorkflowResult success(
                ScopeResult scope,
                ContractAnalysis contracts,
                DocRepairResult docRepair,
                ImplementResult impl,
                CompileResult compile,
                TestResult test) {
            return new WorkflowResult(
                    Phase.TEST,
                    true,
                    scope,
                    contracts,
                    docRepair,
                    impl,
                    compile,
                    test,
                    "Workflow completed successfully");
        }
    }
}
