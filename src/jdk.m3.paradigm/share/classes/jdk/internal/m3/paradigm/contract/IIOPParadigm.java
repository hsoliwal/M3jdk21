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
 * IIOPParadigm - Union of Programming Paradigms.
 *
 * <h2>Paradigm Stack (Order Matters - LOCKED)</h2>
 *
 * <pre>
 * COP → AOP → OOP → FOP → IOP → AnOP
 * absorbing GoF + EIP + Microservice + DAGFlow patterns
 * with compile-time authority
 * </pre>
 *
 * <h2>Paradigm Hierarchy</h2>
 *
 * <pre>
 * IIOPParadigm
 * ├── COP   - Context-Oriented Programming (effects as data, layer activation)
 * ├── AOP   - Aspect-Oriented Programming (compile-time only)
 * ├── OOP   - Object-Oriented Programming (state + invariants)
 * ├── FOP   - Functional Programming (glue, not architecture)
 * ├── IOP   - Interface-Oriented Programming (authority layer)
 * └── AnOP  - Annotation-Oriented Programming (syntax only, compile-time)
 * </pre>
 *
 * @since 1.0 - AI IOP
  * @author Synexia
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the sealed IIOPParadigm metadata variants and the local signatures/defaults category, stackOrder, keyRule, stack, byName. The defaults project metadata or inspect the current variant and do not execute the described application operation.
 * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPParadigm.java}
 * blob {@code db8d658002ccbd493ef713e5b5f219dee759f44a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPParadigm extends IIOPConcept
        permits IIOPParadigm.COP,
                IIOPParadigm.AOP,
                IIOPParadigm.OOP,
                IIOPParadigm.FOP,
                IIOPParadigm.IOP,
                IIOPParadigm.AnOP {

    /**
     * category metadata operation of IIOPParadigm.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the existing enum singleton <code>Category.PARADIGM</code> without runtime enforcement.
     * @m3.dataStructures Declared Category result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
     */
    @Override
    default Category category() {
        return Category.PARADIGM;
    }

    /**
     * Get the order in the paradigm stack.
     *
     * @return stack order (0 = COP, 5 = AnOP)
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract stackOrder interface signature declares a int result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-1898
     * @m3.dataStructures Abstract interface signature returning int. Implementations supply the result and its ownership behavior.
     */
    int stackOrder();

    /**
     * Get the key rule for this paradigm.
     *
     * @return key rule
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract keyRule interface signature declares a String result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-2011
     * @m3.dataStructures Abstract interface signature returning String. Implementations supply the result and its ownership behavior.
     */
    String keyRule();

    // --- COP - Context-Oriented Programming ---

    /**
     * COP metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied COP metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String[] features. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:features} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The features declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-2096; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored features metadata in COP.
     * @m3.member {@code FIELD:KEY_RULE} pattern=synexia:iop; algorithm=Initializes KEY_RULE from the existing expression <code>"All execution happens inside a context"</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One String field with its declared initializer.; description=Stored KEY_RULE metadata in COP.
     */
    record COP(String[] features) implements IIOPParadigm {

        public static final String KEY_RULE = "All execution happens inside a context";

        /**
         * name metadata operation of COP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"COP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "COP";
        }

        /**
         * description metadata operation of COP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Context-Oriented Programming - " + KEY_RULE</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Context-Oriented Programming - " + KEY_RULE;
        }

        /**
         * stackOrder metadata operation of COP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>0</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared int result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public int stackOrder() {
            return 0;
        }

        /**
         * keyRule metadata operation of COP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored KEY_RULE field or record component directly without copying, validation or policy execution.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String keyRule() {
            return KEY_RULE;
        }

        /**
         * standard metadata operation of COP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a fresh COP record and String array containing the existing feature literals in source order. The returned array remains mutable through the generated component accessor.
         * @m3.dataStructures Declared COP result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static COP standard() {
            return new COP(new String[] {
                "Effects as data (Koffect pattern)",
                "Layer activation (ContextJS pattern)",
                "Proceed chain (ContextR pattern)",
                "Dynamic scoping (ContextL pattern)"
            });
        }
    }

    // --- AOP - Aspect-Oriented Programming ---

    /**
     * AOP metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied AOP metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String[] aspects. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:aspects} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The aspects declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-3019; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored aspects metadata in AOP.
     * @m3.member {@code FIELD:KEY_RULE} pattern=synexia:iop; algorithm=Initializes KEY_RULE from the existing expression <code>"AOP ends when wrappers are generated"</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One String field with its declared initializer.; description=Stored KEY_RULE metadata in AOP.
     */
    record AOP(String[] aspects) implements IIOPParadigm {

        public static final String KEY_RULE = "AOP ends when wrappers are generated";

        /**
         * name metadata operation of AOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"AOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "AOP";
        }

        /**
         * description metadata operation of AOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Aspect-Oriented Programming - " + KEY_RULE</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Aspect-Oriented Programming - " + KEY_RULE;
        }

        /**
         * stackOrder metadata operation of AOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>1</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared int result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public int stackOrder() {
            return 1;
        }

        /**
         * keyRule metadata operation of AOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored KEY_RULE field or record component directly without copying, validation or policy execution.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String keyRule() {
            return KEY_RULE;
        }

        /**
         * standard metadata operation of AOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a fresh AOP record and String array containing the existing feature literals in source order. The returned array remains mutable through the generated component accessor.
         * @m3.dataStructures Declared AOP result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static AOP standard() {
            return new AOP(new String[] {
                "Compile-time transformation only", "No runtime weaving", "Cross-cutting concerns as code generation"
            });
        }
    }

    // --- OOP - Object-Oriented Programming ---

    /**
     * OOP metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied OOP metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String[] principles. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:principles} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The principles declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-3842; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored principles metadata in OOP.
     * @m3.member {@code FIELD:KEY_RULE} pattern=synexia:iop; algorithm=Initializes KEY_RULE from the existing expression <code>"Objects own state, not behavior policy"</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One String field with its declared initializer.; description=Stored KEY_RULE metadata in OOP.
     */
    record OOP(String[] principles) implements IIOPParadigm {

        public static final String KEY_RULE = "Objects own state, not behavior policy";

        /**
         * name metadata operation of OOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"OOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "OOP";
        }

        /**
         * description metadata operation of OOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Object-Oriented Programming - " + KEY_RULE</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Object-Oriented Programming - " + KEY_RULE;
        }

        /**
         * stackOrder metadata operation of OOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>2</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared int result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public int stackOrder() {
            return 2;
        }

        /**
         * keyRule metadata operation of OOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored KEY_RULE field or record component directly without copying, validation or policy execution.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String keyRule() {
            return KEY_RULE;
        }

        /**
         * standard metadata operation of OOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a fresh OOP record and String array containing the existing feature literals in source order. The returned array remains mutable through the generated component accessor.
         * @m3.dataStructures Declared OOP result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static OOP standard() {
            return new OOP(new String[] {
                "Encapsulation and invariants",
                "Polymorphism through interfaces",
                "Composition over inheritance",
                "Single responsibility"
            });
        }
    }

    // --- FOP - Functional-Oriented Programming ---

    /**
     * FOP metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied FOP metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String[] features. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:features} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The features declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-4743; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored features metadata in FOP.
     * @m3.member {@code FIELD:KEY_RULE} pattern=synexia:iop; algorithm=Initializes KEY_RULE from the existing expression <code>"If a lambda feels like a program, it\'s wrong"</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One String field with its declared initializer.; description=Stored KEY_RULE metadata in FOP.
     */
    record FOP(String[] features) implements IIOPParadigm {

        public static final String KEY_RULE = "If a lambda feels like a program, it's wrong";

        /**
         * name metadata operation of FOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"FOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "FOP";
        }

        /**
         * description metadata operation of FOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Functional-Oriented Programming - " + KEY_RULE</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Functional-Oriented Programming - " + KEY_RULE;
        }

        /**
         * stackOrder metadata operation of FOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>3</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared int result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public int stackOrder() {
            return 3;
        }

        /**
         * keyRule metadata operation of FOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored KEY_RULE field or record component directly without copying, validation or policy execution.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String keyRule() {
            return KEY_RULE;
        }

        /**
         * standard metadata operation of FOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a fresh FOP record and String array containing the existing feature literals in source order. The returned array remains mutable through the generated component accessor.
         * @m3.dataStructures Declared FOP result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static FOP standard() {
            return new FOP(new String[] {
                "Glue, not architecture",
                "Pure functions for transformations",
                "Streams and Optional for composition",
                "Immutability by default"
            });
        }
    }

    // --- IOP - Interface-Oriented Programming (Authority Layer) ---

    /**
     * IOP metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied IOP metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String[] principles. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:principles} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The principles declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-5676; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored principles metadata in IOP.
     * @m3.member {@code FIELD:KEY_RULE} pattern=synexia:iop; algorithm=Initializes KEY_RULE from the existing expression <code>"Interfaces define what the system is"</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One String field with its declared initializer.; description=Stored KEY_RULE metadata in IOP.
     */
    record IOP(String[] principles) implements IIOPParadigm {

        public static final String KEY_RULE = "Interfaces define what the system is";

        /**
         * name metadata operation of IOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"IOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "IOP";
        }

        /**
         * description metadata operation of IOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Interface-Oriented Programming - " + KEY_RULE</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Interface-Oriented Programming - " + KEY_RULE;
        }

        /**
         * stackOrder metadata operation of IOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>4</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared int result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public int stackOrder() {
            return 4;
        }

        /**
         * keyRule metadata operation of IOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored KEY_RULE field or record component directly without copying, validation or policy execution.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String keyRule() {
            return KEY_RULE;
        }

        /**
         * standard metadata operation of IOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a fresh IOP record and String array containing the existing feature literals in source order. The returned array remains mutable through the generated component accessor.
         * @m3.dataStructures Declared IOP result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static IOP standard() {
            return new IOP(new String[] {
                "JavaDoc becomes the contract",
                "Code is instruction, state, implementation, memory",
                "AI reads interfaces, not implementations",
                "Compiler is the authority"
            });
        }
    }

    // --- AnOP - Annotation-Oriented Programming ---

    /**
     * AnOP metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied AnOP metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String[] rules. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:rules} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The rules declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPParadigm-6615; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored rules metadata in AnOP.
     * @m3.member {@code FIELD:KEY_RULE} pattern=synexia:iop; algorithm=Initializes KEY_RULE from the existing expression <code>"Annotations must behave like @Buffered"</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One String field with its declared initializer.; description=Stored KEY_RULE metadata in AnOP.
     */
    record AnOP(String[] rules) implements IIOPParadigm {

        public static final String KEY_RULE = "Annotations must behave like @Buffered";

        /**
         * name metadata operation of AnOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"AnOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return "AnOP";
        }

        /**
         * description metadata operation of AnOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Annotation-Oriented Programming - " + KEY_RULE</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return "Annotation-Oriented Programming - " + KEY_RULE;
        }

        /**
         * stackOrder metadata operation of AnOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>5</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared int result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public int stackOrder() {
            return 5;
        }

        /**
         * keyRule metadata operation of AnOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the stored KEY_RULE field or record component directly without copying, validation or policy execution.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String keyRule() {
            return KEY_RULE;
        }

        /**
         * standard metadata operation of AnOP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates a fresh AnOP record and String array containing the existing feature literals in source order. The returned array remains mutable through the generated component accessor.
         * @m3.dataStructures Declared AnOP result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static AnOP standard() {
            return new AnOP(new String[] {
                "SOURCE retention only",
                "Compile-time consumed",
                "Eliminated after generation",
                "No runtime presence",
                "NEVER introduce queues, change execution domain, spawn threads"
            });
        }
    }

    // --- UTILITY METHODS ---

    /**
     * Get all paradigms in stack order.
     *
     * @return array of paradigms
     * @m3.pattern synexia:iop
     * @m3.algorithm Creates a fresh array containing COP.standard(), AOP.standard(), OOP.standard(), FOP.standard(), IOP.standard() and AnOP.standard() in that exact order. Each call creates fresh records and feature arrays.
     * @m3.dataStructures Fresh six-element IIOPParadigm array and six new standard records with separate String arrays.
     */
    static IIOPParadigm[] stack() {
        return new IIOPParadigm[] {
            COP.standard(), AOP.standard(), OOP.standard(), FOP.standard(), IOP.standard(), AnOP.standard()
        };
    }

    /**
     * Get paradigm by name.
     *
     * @param name
     *            paradigm name
     * @return paradigm or null
     * @m3.pattern synexia:iop
     * @m3.algorithm Calls name.toUpperCase() using the default locale, switches over COP, AOP, OOP, FOP, IOP and ANOP, and creates the corresponding standard record. Returns null for unmatched names and throws NullPointerException for a null name.
     * @m3.dataStructures One String input, its uppercase String, a switch expression and a newly allocated paradigm record with a fresh String array or null.
     */
    static IIOPParadigm byName(String name) {
        return switch (name.toUpperCase()) {
            case "COP" -> COP.standard();
            case "AOP" -> AOP.standard();
            case "OOP" -> OOP.standard();
            case "FOP" -> FOP.standard();
            case "IOP" -> IOP.standard();
            case "ANOP" -> AnOP.standard();
            default -> null;
        };
    }
}
