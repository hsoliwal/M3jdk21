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

/**
 * IIOPContract - Union of Determinism and Purity Contracts.
 *
 * <h2>Contract Hierarchy</h2>
 *
 * <pre>
 * IIOPContract
 * ├── Deterministic       - Same input → same output
 * ├── SideEffectFree      - Pure function (no side effects)
 * ├── ExternalState       - May touch external state (file, network, clock...)
 * └── Idempotent          - Multiple calls = single call
 * </pre>
 *
 * @since 1.0 - AI IOP
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares four contract metadata variants and returns the CONTRACT category. These declarations perform no determinism, purity, external-access or idempotency enforcement.
 * @m3.dataStructures Sealed interface permitting Deterministic, SideEffectFree, ExternalState and Idempotent records.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPContract.java}
 * blob {@code 7a47eeb88c4c5609f20e8fd24493df668700e772} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPContract extends IIOPConcept
        permits IIOPContract.Deterministic,
                IIOPContract.SideEffectFree,
                IIOPContract.ExternalState,
                IIOPContract.Idempotent {

    /**
     * Returns the existing Category.CONTRACT enum constant.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the existing Category.CONTRACT enum constant.
     * @m3.dataStructures Shared Category.CONTRACT enum reference returned directly. The method body performs no explicit allocation.
     */
    @Override
    default Category category() {
        return Category.CONTRACT;
    }

    /**
     * Check if this contract requires verification.
     *
     * @return true if should be verified
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract interface signature has no method body or algorithm implementation.; review=iop-contract-declarations-20261009/reviewed-declarations.json#IIOPContract-requiresVerification
     * @m3.dataStructures Abstract zero-argument method returning boolean.
     */
    boolean requiresVerification();

    // --- DETERMINISTIC CONTRACT ---

    /**
     * Carries tolerance, verification request, seed and strictness metadata. Supplied values are accepted without validation and do not establish deterministic behavior.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries tolerance, verification request, seed and strictness metadata. Supplied values are accepted without validation and do not establish deterministic behavior.
     * @m3.dataStructures Record with Tolerance reference, boolean verify, long seed and boolean strict components.
     * @m3.member {@code FIELD:tolerance} pattern=synexia:iop; algorithm=Stores the supplied requested output-comparison tolerance, including null, without applying the comparison.; data.structures=Tolerance reference record component.; description=Stores the supplied requested output-comparison tolerance, including null, without applying the comparison.
     * @m3.member {@code FIELD:verify} pattern=synexia:iop; algorithm=Stores the supplied verification request returned by requiresVerification.; data.structures=boolean record component.; description=Stores the supplied verification request returned by requiresVerification.
     * @m3.member {@code FIELD:seed} pattern=synexia:iop; algorithm=Stores the supplied seed hint without initializing or invoking a random generator.; data.structures=long record component.; description=Stores the supplied seed hint without initializing or invoking a random generator.
     * @m3.member {@code FIELD:strict} pattern=synexia:iop; algorithm=Stores the supplied strictness hint without enforcing a policy.; data.structures=boolean record component.; description=Stores the supplied strictness hint without enforcing a policy.
     */
    record Deterministic(Tolerance tolerance, boolean verify, long seed, boolean strict) implements IIOPContract {

        /**
         * Returns the fixed concept name {@code "DETERMINISTIC"}.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed concept name {@code "DETERMINISTIC"}.
         * @m3.dataStructures Existing String literal reference returned directly.
         */
        @Override
        public String name() {
            return "DETERMINISTIC";
        }

        /**
         * Concatenates the fixed determinism-description prefix, the tolerance value and closing parenthesis. A null tolerance is rendered as "null".
         * @m3.pattern synexia:iop
         * @m3.algorithm Concatenates the fixed determinism-description prefix, the tolerance value and closing parenthesis. A null tolerance is rendered as "null".
         * @m3.dataStructures String result produced by concatenation with the Tolerance record component.
         */
        @Override
        public String description() {
            return "Same input → same output (tolerance: " + tolerance + ")";
        }

        /**
         * Returns the stored verify flag without performing verification.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored verify flag without performing verification.
         * @m3.dataStructures boolean projection from the existing record component.
         */
        @Override
        public boolean requiresVerification() {
            return verify;
        }

        /**
         * Enumerates exact-output, semantic-equivalence and behavioral-equivalence comparison requests without comparing outputs.
         * @m3.pattern synexia:iop
         * @m3.algorithm Enumerates exact-output, semantic-equivalence and behavioral-equivalence comparison requests without comparing outputs.
         * @m3.dataStructures Enum constants EXACT, SEMANTICALLY_EQUIVALENT and BEHAVIORALLY_EQUIVALENT in existing declaration order.
         * @m3.member {@code FIELD:EXACT} pattern=synexia:iop; algorithm=Names the EXACT metadata category without implementing that category or its policy.; data.structures=Existing Tolerance enum constant with its identity and declaration order preserved.
         * @m3.member {@code FIELD:SEMANTICALLY_EQUIVALENT} pattern=synexia:iop; algorithm=Names the SEMANTICALLY_EQUIVALENT metadata category without implementing that category or its policy.; data.structures=Existing Tolerance enum constant with its identity and declaration order preserved.
         * @m3.member {@code FIELD:BEHAVIORALLY_EQUIVALENT} pattern=synexia:iop; algorithm=Names the BEHAVIORALLY_EQUIVALENT metadata category without implementing that category or its policy.; data.structures=Existing Tolerance enum constant with its identity and declaration order preserved.
         */
        public enum Tolerance {
            /** Byte-for-byte identical output. */
            EXACT,
            /** Same AST structure, may differ in whitespace. */
            SEMANTICALLY_EQUIVALENT,
            /** Same behavior, implementation may vary. */
            BEHAVIORALLY_EQUIVALENT
        }

        /**
         * Creates a Deterministic record with EXACT tolerance, verify true, seed 0L and strict true.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a Deterministic record with EXACT tolerance, verify true, seed 0L and strict true.
         * @m3.dataStructures Fresh Deterministic record with existing enum reference and primitive component values.
         */
        public static Deterministic strictMode() {
            return new Deterministic(Tolerance.EXACT, true, 0L, true);
        }

        /**
         * Creates a Deterministic record with SEMANTICALLY_EQUIVALENT tolerance, verify false, seed 0L and strict true.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a Deterministic record with SEMANTICALLY_EQUIVALENT tolerance, verify false, seed 0L and strict true.
         * @m3.dataStructures Fresh Deterministic record with existing enum reference and primitive component values.
         */
        public static Deterministic semanticMode() {
            return new Deterministic(Tolerance.SEMANTICALLY_EQUIVALENT, false, 0L, true);
        }

        /**
         * Creates a Deterministic record with BEHAVIORALLY_EQUIVALENT tolerance, verify false, seed 0L and strict false.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a Deterministic record with BEHAVIORALLY_EQUIVALENT tolerance, verify false, seed 0L and strict false.
         * @m3.dataStructures Fresh Deterministic record with existing enum reference and primitive component values.
         */
        public static Deterministic behavioralMode() {
            return new Deterministic(Tolerance.BEHAVIORALLY_EQUIVALENT, false, 0L, false);
        }
    }

    // --- SIDE-EFFECT-FREE CONTRACT ---

    /**
     * Carries allowed-effect flags, a verification request and named pure/impure hints. Construction performs no effect analysis or enforcement.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries allowed-effect flags, a verification request and named pure/impure hints. Construction performs no effect analysis or enforcement.
     * @m3.dataStructures Record with five boolean components and two caller-supplied String array references. Arrays are retained and returned directly, including null.
     * @m3.member {@code FIELD:allowLogging} pattern=synexia:iop; algorithm=Stores whether the metadata permits logging without performing or intercepting logging.; data.structures=boolean record component.; description=Stores whether the metadata permits logging without performing or intercepting logging.
     * @m3.member {@code FIELD:allowExceptions} pattern=synexia:iop; algorithm=Stores whether the metadata permits exceptions without changing exception behavior.; data.structures=boolean record component.; description=Stores whether the metadata permits exceptions without changing exception behavior.
     * @m3.member {@code FIELD:allowEnvReads} pattern=synexia:iop; algorithm=Stores whether the metadata permits environment reads without reading the environment.; data.structures=boolean record component.; description=Stores whether the metadata permits environment reads without reading the environment.
     * @m3.member {@code FIELD:allowThreadCreation} pattern=synexia:iop; algorithm=Stores whether the metadata permits thread creation without creating or checking threads.; data.structures=boolean record component.; description=Stores whether the metadata permits thread creation without creating or checking threads.
     * @m3.member {@code FIELD:verify} pattern=synexia:iop; algorithm=Stores the supplied verification request returned by requiresVerification.; data.structures=boolean record component.; description=Stores the supplied verification request returned by requiresVerification.
     * @m3.member {@code FIELD:knownPure} pattern=synexia:iop; algorithm=Retains supplied pure-name hints without validating names or proving purity.; data.structures=Caller-supplied String array reference record component, retained and exposed directly including null.; description=Retains supplied pure-name hints without validating names or proving purity.
     * @m3.member {@code FIELD:knownImpure} pattern=synexia:iop; algorithm=Retains supplied impure-name hints without validating names or analyzing effects.; data.structures=Caller-supplied String array reference record component, retained and exposed directly including null.; description=Retains supplied impure-name hints without validating names or analyzing effects.
     */
    record SideEffectFree(
            boolean allowLogging,
            boolean allowExceptions,
            boolean allowEnvReads,
            boolean allowThreadCreation,
            boolean verify,
            String[] knownPure,
            String[] knownImpure)
            implements IIOPContract {

        /**
         * Returns the fixed concept name {@code "SIDE_EFFECT_FREE"}.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed concept name {@code "SIDE_EFFECT_FREE"}.
         * @m3.dataStructures Existing String literal reference returned directly.
         */
        @Override
        public String name() {
            return "SIDE_EFFECT_FREE";
        }

        /**
         * Returns the fixed description {@code "Pure function with no side effects"} without checking whether a function is pure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed description {@code "Pure function with no side effects"} without checking whether a function is pure.
         * @m3.dataStructures Existing String literal reference returned directly.
         */
        @Override
        public String description() {
            return "Pure function with no side effects";
        }

        /**
         * Returns the stored verify flag without performing verification.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored verify flag without performing verification.
         * @m3.dataStructures boolean projection from the existing record component.
         */
        @Override
        public boolean requiresVerification() {
            return verify;
        }

        /**
         * Creates flags false, false, false, false and verify true, with separate fresh empty pure and impure hint arrays.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates flags false, false, false, false and verify true, with separate fresh empty pure and impure hint arrays.
         * @m3.dataStructures Fresh SideEffectFree record and two distinct fresh zero-length String arrays.
         */
        public static SideEffectFree strict() {
            return new SideEffectFree(false, false, false, false, true, new String[0], new String[0]);
        }

        /**
         * Creates flags false, true, false, false and verify true, with separate fresh empty pure and impure hint arrays.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates flags false, true, false, false and verify true, with separate fresh empty pure and impure hint arrays.
         * @m3.dataStructures Fresh SideEffectFree record and two distinct fresh zero-length String arrays.
         */
        public static SideEffectFree standard() {
            return new SideEffectFree(false, true, false, false, true, new String[0], new String[0]);
        }

        /**
         * Creates flags true, true, true, false and verify false, with separate fresh empty pure and impure hint arrays.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates flags true, true, true, false and verify false, with separate fresh empty pure and impure hint arrays.
         * @m3.dataStructures Fresh SideEffectFree record and two distinct fresh zero-length String arrays.
         */
        public static SideEffectFree relaxed() {
            return new SideEffectFree(true, true, true, false, false, new String[0], new String[0]);
        }
    }

    // --- EXTERNAL STATE CONTRACT ---

    /**
     * Carries external-state categories, access flags and retry/timeout hints. Construction neither accesses external state nor implements timeouts, retries or mocking.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries external-state categories, access flags and retry/timeout hints. Construction neither accesses external state nor implements timeouts, retries or mocking.
     * @m3.dataStructures Record with one caller-supplied ExternalStateType array reference, four boolean flags and two int values. The array is retained and returned directly, including null.
     * @m3.member {@code FIELD:types} pattern=synexia:iop; algorithm=Retains supplied external-state category hints without checking or accessing resources.; data.structures=Caller-supplied ExternalStateType array reference record component, retained and exposed directly including null.; description=Retains supplied external-state category hints without checking or accessing resources.
     * @m3.member {@code FIELD:readOnly} pattern=synexia:iop; algorithm=Stores the supplied read-only access hint without enforcing resource permissions.; data.structures=boolean record component.; description=Stores the supplied read-only access hint without enforcing resource permissions.
     * @m3.member {@code FIELD:requireMocking} pattern=synexia:iop; algorithm=Stores the supplied mocking request returned by requiresVerification without creating mocks.; data.structures=boolean record component.; description=Stores the supplied mocking request returned by requiresVerification without creating mocks.
     * @m3.member {@code FIELD:logAccess} pattern=synexia:iop; algorithm=Stores the supplied access-logging hint without emitting logs.; data.structures=boolean record component.; description=Stores the supplied access-logging hint without emitting logs.
     * @m3.member {@code FIELD:timeoutMs} pattern=synexia:iop; algorithm=Stores the supplied timeout hint without validating the value or scheduling a timeout.; data.structures=int record component carrying the millisecond timeout hint.; description=Stores the supplied timeout hint without validating the value or scheduling a timeout.
     * @m3.member {@code FIELD:retryOnFailure} pattern=synexia:iop; algorithm=Stores the supplied retry hint without performing retries.; data.structures=boolean record component.; description=Stores the supplied retry hint without performing retries.
     * @m3.member {@code FIELD:maxRetries} pattern=synexia:iop; algorithm=Stores the supplied retry-count hint without validating the count or executing retries.; data.structures=int record component.; description=Stores the supplied retry-count hint without validating the count or executing retries.
     */
    record ExternalState(
            ExternalStateType[] types,
            boolean readOnly,
            boolean requireMocking,
            boolean logAccess,
            int timeoutMs,
            boolean retryOnFailure,
            int maxRetries)
            implements IIOPContract {

        /**
         * Returns the fixed concept name {@code "EXTERNAL_STATE"}.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed concept name {@code "EXTERNAL_STATE"}.
         * @m3.dataStructures Existing String literal reference returned directly.
         */
        @Override
        public String name() {
            return "EXTERNAL_STATE";
        }

        /**
         * Concatenates the fixed external-state-description prefix with Arrays.toString(types), preserving array order and rendering null arrays or elements as "null".
         * @m3.pattern synexia:iop
         * @m3.algorithm Concatenates the fixed external-state-description prefix with Arrays.toString(types), preserving array order and rendering null arrays or elements as "null".
         * @m3.dataStructures String result from formatting the existing ExternalStateType array reference and concatenating the prefix.
         */
        @Override
        public String description() {
            return "May access external state: " + java.util.Arrays.toString(types);
        }

        /**
         * Returns the stored requireMocking flag without performing verification.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored requireMocking flag without performing verification.
         * @m3.dataStructures boolean projection from the existing record component.
         */
        @Override
        public boolean requiresVerification() {
            return requireMocking;
        }

        /**
         * Enumerates fourteen external-state categories without accessing any of those resources.
         * @m3.pattern synexia:iop
         * @m3.algorithm Enumerates fourteen external-state categories without accessing any of those resources.
         * @m3.dataStructures Enum constants FILE_SYSTEM, NETWORK, DATABASE, MESSAGE_QUEUE, SYSTEM_CLOCK, RANDOM, ENVIRONMENT, CONSOLE, PROCESS, NATIVE, THREADING, SECURITY, GUI and ANY.
         * @m3.member {@code FIELD:FILE_SYSTEM} pattern=synexia:iop; algorithm=Names the FILE_SYSTEM metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the FILE_SYSTEM metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:NETWORK} pattern=synexia:iop; algorithm=Names the NETWORK metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the NETWORK metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:DATABASE} pattern=synexia:iop; algorithm=Names the DATABASE metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the DATABASE metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:MESSAGE_QUEUE} pattern=synexia:iop; algorithm=Names the MESSAGE_QUEUE metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the MESSAGE_QUEUE metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:SYSTEM_CLOCK} pattern=synexia:iop; algorithm=Names the SYSTEM_CLOCK metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the SYSTEM_CLOCK metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:RANDOM} pattern=synexia:iop; algorithm=Names the RANDOM metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the RANDOM metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:ENVIRONMENT} pattern=synexia:iop; algorithm=Names the ENVIRONMENT metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the ENVIRONMENT metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:CONSOLE} pattern=synexia:iop; algorithm=Names the CONSOLE metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the CONSOLE metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:PROCESS} pattern=synexia:iop; algorithm=Names the PROCESS metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the PROCESS metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:NATIVE} pattern=synexia:iop; algorithm=Names the NATIVE metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the NATIVE metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:THREADING} pattern=synexia:iop; algorithm=Names the THREADING metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the THREADING metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:SECURITY} pattern=synexia:iop; algorithm=Names the SECURITY metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the SECURITY metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:GUI} pattern=synexia:iop; algorithm=Names the GUI metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the GUI metadata category without implementing that category or its policy.
         * @m3.member {@code FIELD:ANY} pattern=synexia:iop; algorithm=Names the ANY metadata category without implementing that category or its policy.; data.structures=Existing ExternalStateType enum constant with its identity and declaration order preserved.; description=Names the ANY metadata category without implementing that category or its policy.
         */
        public enum ExternalStateType {
            FILE_SYSTEM,
            NETWORK,
            DATABASE,
            MESSAGE_QUEUE,
            SYSTEM_CLOCK,
            RANDOM,
            ENVIRONMENT,
            CONSOLE,
            PROCESS,
            NATIVE,
            THREADING,
            SECURITY,
            GUI,
            ANY
        }

        /**
         * Creates a FILE_SYSTEM hint with readOnly false, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure true and maxRetries 3.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a FILE_SYSTEM hint with readOnly false, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure true and maxRetries 3.
         * @m3.dataStructures Fresh ExternalState record and fresh one-element ExternalStateType array.
         */
        public static ExternalState fileSystem() {
            return new ExternalState(
                    new ExternalStateType[] {ExternalStateType.FILE_SYSTEM}, false, true, false, 30000, true, 3);
        }

        /**
         * Creates a NETWORK hint with readOnly false, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure true and maxRetries 3.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a NETWORK hint with readOnly false, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure true and maxRetries 3.
         * @m3.dataStructures Fresh ExternalState record and fresh one-element ExternalStateType array.
         */
        public static ExternalState network() {
            return new ExternalState(
                    new ExternalStateType[] {ExternalStateType.NETWORK}, false, true, false, 30000, true, 3);
        }

        /**
         * Creates a DATABASE hint with readOnly false, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure true and maxRetries 3.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a DATABASE hint with readOnly false, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure true and maxRetries 3.
         * @m3.dataStructures Fresh ExternalState record and fresh one-element ExternalStateType array.
         */
        public static ExternalState database() {
            return new ExternalState(
                    new ExternalStateType[] {ExternalStateType.DATABASE}, false, true, false, 30000, true, 3);
        }

        /**
         * Creates readOnly true, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure false and maxRetries 0 while retaining the supplied varargs array directly.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates readOnly true, requireMocking true, logAccess false, timeoutMs 30000, retryOnFailure false and maxRetries 0 while retaining the supplied varargs array directly.
         * @m3.dataStructures Fresh ExternalState record sharing the supplied ExternalStateType array reference, including null.
         */
        public static ExternalState readOnly(ExternalStateType... types) {
            return new ExternalState(types, true, true, false, 30000, false, 0);
        }
    }

    // --- IDEMPOTENT CONTRACT ---

    /**
     * Carries scope, optional key, TTL and verification metadata. Construction creates no request/session state, key cache or expiration behavior.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries scope, optional key, TTL and verification metadata. Construction creates no request/session state, key cache or expiration behavior.
     * @m3.dataStructures Record with IdempotencyScope and String references, long ttlMs and boolean verify components. References may be null and numeric values are not validated.
     * @m3.member {@code FIELD:scope} pattern=synexia:iop; algorithm=Stores the supplied idempotency scope hint without applying a deduplication policy.; data.structures=IdempotencyScope reference record component, including null.; description=Stores the supplied idempotency scope hint without applying a deduplication policy.
     * @m3.member {@code FIELD:idempotencyKey} pattern=synexia:iop; algorithm=Stores the supplied optional key without creating a cache or deduplicating calls.; data.structures=String reference record component, including null.; description=Stores the supplied optional key without creating a cache or deduplicating calls.
     * @m3.member {@code FIELD:ttlMs} pattern=synexia:iop; algorithm=Stores the supplied lifetime hint without validating the value or implementing expiration.; data.structures=long record component carrying the millisecond lifetime hint.; description=Stores the supplied lifetime hint without validating the value or implementing expiration.
     * @m3.member {@code FIELD:verify} pattern=synexia:iop; algorithm=Stores the supplied verification request returned by requiresVerification.; data.structures=boolean record component.; description=Stores the supplied verification request returned by requiresVerification.
     */
    record Idempotent(IdempotencyScope scope, String idempotencyKey, long ttlMs, boolean verify)
            implements IIOPContract {

        /**
         * Returns the fixed concept name {@code "IDEMPOTENT"}.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed concept name {@code "IDEMPOTENT"}.
         * @m3.dataStructures Existing String literal reference returned directly.
         */
        @Override
        public String name() {
            return "IDEMPOTENT";
        }

        /**
         * Concatenates the fixed idempotency-description prefix, the scope value and closing parenthesis. A null scope is rendered as "null".
         * @m3.pattern synexia:iop
         * @m3.algorithm Concatenates the fixed idempotency-description prefix, the scope value and closing parenthesis. A null scope is rendered as "null".
         * @m3.dataStructures String result produced by concatenation with the IdempotencyScope record component.
         */
        @Override
        public String description() {
            return "Multiple calls = single call (scope: " + scope + ")";
        }

        /**
         * Returns the stored verify flag without performing verification.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored verify flag without performing verification.
         * @m3.dataStructures boolean projection from the existing record component.
         */
        @Override
        public boolean requiresVerification() {
            return verify;
        }

        /**
         * Enumerates request, session and global scope hints without deduplicating operations.
         * @m3.pattern synexia:iop
         * @m3.algorithm Enumerates request, session and global scope hints without deduplicating operations.
         * @m3.dataStructures Enum constants REQUEST, SESSION and GLOBAL in existing declaration order.
         * @m3.member {@code FIELD:REQUEST} pattern=synexia:iop; algorithm=Names the REQUEST metadata category without implementing that category or its policy.; data.structures=Existing IdempotencyScope enum constant with its identity and declaration order preserved.
         * @m3.member {@code FIELD:SESSION} pattern=synexia:iop; algorithm=Names the SESSION metadata category without implementing that category or its policy.; data.structures=Existing IdempotencyScope enum constant with its identity and declaration order preserved.
         * @m3.member {@code FIELD:GLOBAL} pattern=synexia:iop; algorithm=Names the GLOBAL metadata category without implementing that category or its policy.; data.structures=Existing IdempotencyScope enum constant with its identity and declaration order preserved.
         */
        public enum IdempotencyScope {
            /** Idempotent within single request. */
            REQUEST,
            /** Idempotent within session. */
            SESSION,
            /** Idempotent globally with TTL. */
            GLOBAL
        }

        /**
         * Creates REQUEST scope with null key, ttlMs 0 and verify false.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates REQUEST scope with null key, ttlMs 0 and verify false.
         * @m3.dataStructures Fresh Idempotent record with existing enum reference, null String reference and primitive values.
         */
        public static Idempotent request() {
            return new Idempotent(IdempotencyScope.REQUEST, null, 0, false);
        }

        /**
         * Creates SESSION scope with null key, ttlMs 0 and verify false.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates SESSION scope with null key, ttlMs 0 and verify false.
         * @m3.dataStructures Fresh Idempotent record with existing enum reference, null String reference and primitive values.
         */
        public static Idempotent session() {
            return new Idempotent(IdempotencyScope.SESSION, null, 0, false);
        }

        /**
         * Creates GLOBAL scope with the supplied key and ttlMs and verify true without validating either input.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates GLOBAL scope with the supplied key and ttlMs and verify true without validating either input.
         * @m3.dataStructures Fresh Idempotent record retaining the supplied String reference, including null, and long lifetime value.
         */
        public static Idempotent global(String key, long ttlMs) {
            return new Idempotent(IdempotencyScope.GLOBAL, key, ttlMs, true);
        }
    }
}
