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
 * IOP_TYPE_HIERARCHY - The Mandatory Type Hierarchy for IOP.
 *
 * <h2>🔒🔒🔒 LOCKED - SOLID + PROGRAMMING BY INTERFACES 🔒🔒🔒</h2>
 *
 * <h2>THE MANDATORY THREE-LAYER HIERARCHY</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  EVERY IOP TYPE MUST HAVE THREE LAYERS:                                   ║
 * ║                                                                           ║
 * ║  1. I{Name}          → Interface (contract, authority)                    ║
 * ║  2. Abstract{Name}   → Abstract class (shared implementation)             ║
 * ║  3. {Name}Impl       → Final class (concrete, can be empty stub)          ║
 * ║                                                                           ║
 * ║  THIS IS NON-NEGOTIABLE.                                                  ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>THE STRUCTURE</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  LAYER 1: INTERFACE (I{Name})                                           │
 * │  ─────────────────────────────                                          │
 * │                                                                         │
 * │  • Prefixed with "I" (e.g., IContext, IJob, ILayer)                     │
 * │  • PURE CONTRACT - no implementation                                    │
 * │  • Contains JavaDoc specification                                       │
 * │  • Contains @ISpec annotation                                           │
 * │  • May contain static factory methods                                   │
 * │  • May contain default methods (ONLY for true defaults)                 │
 * │  • May contain sealed permits clause                                    │
 * │                                                                         │
 * │  public interface IContext&lt;Self extends IContext&lt;Self>> extends IContext&lt;Self> {                                            │
 * │      String id();                                                       │
 * │      IContext parent();                                                 │
 * │      static IContext create() { return new ContextImpl(); }             │
 * │  }                                                                      │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                                     │
 *                                     ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  LAYER 2: ABSTRACT CLASS (Abstract{Name})                               │
 * │  ────────────────────────────────────────                               │
 * │                                                                         │
 * │  • Prefixed with "Abstract" (e.g., AbstractContext, AbstractJob)        │
 * │  • Implements the interface                                             │
 * │  • Contains SHARED implementation code                                  │
 * │  • Contains protected fields                                            │
 * │  • Contains template methods                                            │
 * │  • May be empty (just implements interface)                             │
 * │  • CANNOT be instantiated directly                                      │
 * │                                                                         │
 * │  public abstract class AbstractContext implements IContext {            │
 * │      protected final String id;                                         │
 * │      protected AbstractContext(String id) { this.id = id; }             │
 * │      @Override public String id() { return id; }                        │
 * │  }                                                                      │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                                     │
 *                                     ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  LAYER 3: FINAL IMPLEMENTATION ({Name}Impl or Concrete{Name})           │
 * │  ──────────────────────────────────────────────────────────             │
 * │                                                                         │
 * │  • Suffixed with "Impl" or prefixed with "Default/Concrete"             │
 * │  • Extends the abstract class                                           │
 * │  • Contains CONCRETE implementation                                     │
 * │  • Should be FINAL (no further extension)                               │
 * │  • CAN BE EMPTY STUB (just extends abstract)                            │
 * │  • This is where LLM generates method bodies                            │
 * │                                                                         │
 * │  public final class ContextImpl extends AbstractContext {               │
 * │      public ContextImpl() { super(UUID.randomUUID().toString()); }      │
 * │      @Override public IContext parent() { return null; }                │
 * │  }                                                                      │
 * │                                                                         │
 * │  // OR EMPTY STUB (VALID):                                              │
 * │  public final class ContextImpl extends AbstractContext {               │
 * │      public ContextImpl() { super(""); }                                │
 * │  }                                                                      │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>WHY THIS PATTERN (SOLID PRINCIPLES)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  D - DEPENDENCY INVERSION PRINCIPLE                                       ║
 * ║  ──────────────────────────────────                                       ║
 * ║  "Depend on abstractions, not concretions."                               ║
 * ║                                                                           ║
 * ║  • All code depends on IContext, not ContextImpl                          ║
 * ║  • Implementations can be swapped without changing clients                ║
 * ║  • Testing: inject mocks at interface level                               ║
 * ║                                                                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  I - INTERFACE SEGREGATION PRINCIPLE                                      ║
 * ║  ───────────────────────────────────                                      ║
 * ║  "Clients should not depend on interfaces they don't use."                ║
 * ║                                                                           ║
 * ║  • Small, focused interfaces (IReader, IWriter vs IReadWriter)            ║
 * ║  • Abstract class can implement multiple interfaces                       ║
 * ║  • Clients depend only on what they need                                  ║
 * ║                                                                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  O - OPEN/CLOSED PRINCIPLE                                                ║
 * ║  ─────────────────────────────                                            ║
 * ║  "Open for extension, closed for modification."                           ║
 * ║                                                                           ║
 * ║  • Interface is CLOSED (contract doesn't change)                          ║
 * ║  • New implementations are OPEN (extend AbstractX)                        ║
 * ║  • Abstract class provides extension points                               ║
 * ║                                                                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  L - LISKOV SUBSTITUTION PRINCIPLE                                        ║
 * ║  ───────────────────────────────                                          ║
 * ║  "Subtypes must be substitutable for their base types."                   ║
 * ║                                                                           ║
 * ║  • Any IContext impl can replace any other                                ║
 * ║  • Abstract class enforces invariants                                     ║
 * ║  • Final class just provides concrete behavior                            ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>TEMPLATE GENERATION (AUTOMATIC)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  Given: @ISpec interface IUserService                                     ║
 * ║                                                                           ║
 * ║  CPU/Template AUTOMATICALLY generates:                                    ║
 * ║  ─────────────────────────────────────                                    ║
 * ║                                                                           ║
 * ║  1. IUserService.java        → Interface (from @ISpec)                    ║
 * ║  2. AbstractUserService.java → Abstract class (skeleton)                  ║
 * ║  3. UserServiceImpl.java     → Final class (LLM fills bodies)             ║
 * ║                                                                           ║
 * ║  All three files are created together.                                    ║
 * ║  Partial generation is a BUG.                                             ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>NAMING CONVENTIONS (LOCKED)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  TYPE                │ PREFIX/SUFFIX    │ EXAMPLE                         ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  Interface           │ I{Name}          │ IContext, IJob, ILayer          ║
 * ║  Abstract Class      │ Abstract{Name}   │ AbstractContext, AbstractJob    ║
 * ║  Final Implementation│ {Name}Impl       │ ContextImpl, JobImpl            ║
 * ║  Default Impl        │ Default{Name}    │ DefaultContext (alternative)    ║
 * ║  Factory             │ {Name}Factory    │ ContextFactory                  ║
 * ║  Builder             │ {Name}Builder    │ ContextBuilder                  ║
 * ║  AnOP Annotation     │ IAI{Name}        │ IAIHint, IAIPattern             ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>EXAMPLE: COMPLETE TYPE HIERARCHY</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	// LAYER 1: INTERFACE
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	&#64;ISpec(value = "User management service")
 * 	public interface IUserService&lt;Self extends IUserService&lt;Self>> extends IContext&lt;Self> {
 *
 * 		User findById(String id);
 *
 * 		User save(User user);
 *
 * 		void delete(String id);
 *
 * 		static IUserService create() {
 * 			return new UserServiceImpl();
 * 		}
 * 	}
 *
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	// LAYER 2: ABSTRACT CLASS
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	public abstract class AbstractUserService implements IUserService {
 *
 * 		protected final Logger log = LoggerFactory.getLogger(getClass());
 *
 * 		// Template method - subclass provides repository
 * 		protected abstract UserRepository repository();
 *
 * 		&#64;Override
 * 		public User findById(String id) {
 * 			log.debug("Finding user: {}", id);
 * 			return repository().findById(id).orElse(null);
 * 		}
 *
 * 		// Leave save() and delete() abstract for concrete implementation
 * 	}
 *
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	// LAYER 3: FINAL IMPLEMENTATION
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	&#64;IAIGenerated(model = Model.ModelType.SONNET)
 * 	public final class UserServiceImpl extends AbstractUserService {
 *
 * 		private final UserRepository userRepository;
 *
 * 		public UserServiceImpl() {
 * 			this.userRepository = new JpaUserRepository();
 * 		}
 *
 * 		&#64;Override
 * 		protected UserRepository repository() {
 * 			return userRepository;
 * 		}
 *
 * 		&#64;Override
 * 		public User save(User user) {
 * 			// LLM generates this body
 * 			return userRepository.save(user);
 * 		}
 *
 * 		&#64;Override
 * 		public void delete(String id) {
 * 			// LLM generates this body
 * 			userRepository.deleteById(id);
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - Type Hierarchy
 * @see IOP_GENERATION_LAW
  * @author Synexia
 * @m3.pattern synexia:iop
 * @m3.algorithm Groups exact hierarchy-law text, ordered naming-pattern descriptors, a fresh-array name generator and a case-sensitive prefix/equality validator. It does not inspect classes or enforce the textual generation laws.
 * @m3.dataStructures Four static String constants, a nested three-value enum with two String fields, String arguments and freshly allocated three-element arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IOP_TYPE_HIERARCHY.java}
 * blob {@code be320d61b6cbeed153fd9d047e3b5cb86a6b0258} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IOP_TYPE_HIERARCHY {

    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body, preventing ordinary caller construction.
     * @m3.dataStructures No constructor parameters or declared instance state.
     */
    private IOP_TYPE_HIERARCHY() {}

    // --- THE IMMUTABLE RULES ---

    /** Rule 1: Every IOP type has three layers.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_1 = """
			Every IOP type MUST have:
			1. I{Name} - Interface (contract)
			2. Abstract{Name} - Abstract class (shared impl)
			3. {Name}Impl - Final class (concrete, can be empty)
			""";

    /** Rule 2: Depend on interfaces.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_2 = """
			All code depends on I{Name}, never {Name}Impl.
			Constructor injection uses interface types.
			Tests mock at interface level.
			""";

    /** Rule 3: Abstract class is the skeleton.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_3 = """
			Abstract class contains:
			- Protected fields
			- Template methods
			- Shared logic
			- Logging/metrics hooks
			""";

    /** Rule 4: Final class can be empty.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String RULE_4 = """
			Final implementation CAN BE EMPTY STUB.
			Empty stub is valid - just extends abstract.
			LLM fills method bodies later.
			Partial hierarchy is a BUG.
			""";

    // --- TYPE LAYERS ---

    /** The three mandatory layers.
     * @m3.pattern synexia:iop
     * @m3.algorithm Stores three literal pattern/description pairs in source order, formats base names through String.format and returns the stored description. No Java identifier validation occurs.
     * @m3.dataStructures Three enum singleton values, each with final pattern and description String references.
     */
    public enum Layer {

        /** Interface - the contract.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the INTERFACE singleton from exact formatting pattern "I%s" and description "Interface contract - what the system promises" at its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        INTERFACE("I%s", "Interface contract - what the system promises"),

        /** Abstract class - shared implementation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ABSTRACT singleton from exact formatting pattern "Abstract%s" and description "Shared implementation - template methods" at its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        ABSTRACT("Abstract%s", "Shared implementation - template methods"),

        /** Final implementation - concrete.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the FINAL singleton from exact formatting pattern "%sImpl" and description "Concrete implementation - LLM fills bodies" at its original ordinal position.
         * @m3.dataStructures One enum singleton and two final String references.
         */
        FINAL("%sImpl", "Concrete implementation - LLM fills bodies");

        /**
         * Immutable pattern text of a layer descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied pattern String value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final String field per instance.
         */
        private final String pattern;
        /**
         * Immutable description text of a layer descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied description String value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final String field per instance.
         */
        private final String description;

        /**
         * Initializes the naming-pattern and description fields.
         * @m3.pattern synexia:bop
         * @m3.algorithm Assigns the supplied pattern and description references to their final fields without validation or copying.
         * @m3.dataStructures Two String parameters and two final String instance fields.
         */
        Layer(String pattern, String description) {
            this.pattern = pattern;
            this.description = description;
        }

        /**
         * Layer name formatting helper.
         * @m3.pattern synexia:bop
         * @m3.algorithm Calls String.format(pattern, baseName) using the default formatting locale and the stored single-string-substitution pattern. Null input formats as null text, and percent characters in the supplied baseName remain argument text.
         * @m3.dataStructures Stored String pattern, one String argument, formatter varargs and the formatted String result.
         */
        public String format(String baseName) {
            return String.format(pattern, baseName);
        }

        /**
         * Accessor for the stored description value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing description field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored String value and its returned value.
         */
        public String description() {
            return description;
        }
    }

    /**
     * Generates all three layer names from a base name.
     *
     * @param baseName
     *            the base name (e.g., "UserService")
     * @return array of [interface, abstract, impl] names
     * @m3.pattern synexia:bop
     * @m3.algorithm Calls Layer.INTERFACE.format, Layer.ABSTRACT.format and Layer.FINAL.format in that source order, then returns their results in a fresh array. Null names format as null text, and no identifier validation or file IO occurs.
     * @m3.dataStructures One String input, three formatted String results and a new three-element String array.
     */
    public static String[] generateHierarchy(String baseName) {
        return new String[] {
            Layer.INTERFACE.format(baseName), // IUserService
            Layer.ABSTRACT.format(baseName), // AbstractUserService
            Layer.FINAL.format(baseName) // UserServiceImpl
        };
    }

    /**
     * Validates that a hierarchy is complete.
     *
     * @param interfaceName
     *            the interface name
     * @param abstractName
     *            the abstract class name
     * @param implName
     *            the implementation name
     * @return true if valid hierarchy
     * @m3.pattern synexia:bop
     * @m3.algorithm Dereferences interfaceName, returns false if it lacks prefix I or has length below two, then extracts the suffix and checks abstractName against Abstract plus suffix before implName against suffix plus Impl. Null references throw only when their branch is reached.
     * @m3.dataStructures Three String inputs, one substring base name, concatenated expected names and a boolean result.
     */
    public static boolean isValidHierarchy(String interfaceName, String abstractName, String implName) {
        // Interface must start with I
        if (!interfaceName.startsWith("I") || interfaceName.length() < 2) {
            return false;
        }

        String baseName = interfaceName.substring(1); // Remove I prefix

        // Abstract must be Abstract{baseName}
        if (!abstractName.equals("Abstract" + baseName)) {
            return false;
        }

        // Impl must be {baseName}Impl
        if (!implName.equals(baseName + "Impl")) {
            return false;
        }

        return true;
    }
}
