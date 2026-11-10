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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Dependency-free Java 21 primitives for all 23 Gang-of-Four patterns.
 *
 * <p>The types are intentionally small contracts, not framework replacements.</p>
 * @m3.pattern synexia:iop
 * @m3.algorithm Expose nested role types and an enum inventory without constructing a framework.
 * @m3.dataStructures Nested interfaces, classes and records plus EnumSet of GoF.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/patterns/GoFPatterns.java}
 * blob {@code 15eda20aea2b45884da8da1f9a23d20e7cef59da} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class GoFPatterns {
    /**
     * Prevents ordinary construction of the namespace holder.
     * @m3.pattern synexia:iop
     * @m3.algorithm Private empty constructor performs no initialization beyond Object construction.
     * @m3.dataStructures No instance fields in the namespace holder.
     */
    private GoFPatterns() {}

    /**
     * Returns all declared GoF enum values as a fresh mutable set.
     * @m3.pattern synexia:iop
     * @m3.algorithm Call EnumSet.allOf on GoF.class, without testing implementation behavior.
     * @m3.dataStructures New EnumSet of GoF constants.
     */
    public static EnumSet<GoF> implementedPatterns() {
        return EnumSet.allOf(GoF.class);
    }

    // ---------------------------------------------------------------------
    // Creational (5)
    // ---------------------------------------------------------------------

    /**
     * Factory contract for producing one product type.
     * @m3.pattern gof:factory-method
     * @m3.algorithm Delegate product construction policy to the create implementation.
     * @m3.dataStructures Generic product type T and no interface-owned storage.
     */
    @FunctionalInterface
    public interface FactoryMethod<T> {
        /**
         * Produces a product according to the implementation's construction policy.
         * @m3.pattern gof:factory-method
         * @m3.algorithm Abstract creation operation with allocation and reuse left to the implementer.
         * @m3.dataStructures Returned T product reference.
         */
        T create();
    }

    /**
     * Factory contract for a pair of related product types.
     * @m3.pattern gof:abstract-factory
     * @m3.algorithm Separate primary and secondary creation operations supplied by an implementation.
     * @m3.dataStructures Generic product types A and B with no interface-owned state.
     */
    public interface AbstractFactory<A, B> {
        /**
         * Produces the primary product of the family.
         * @m3.pattern gof:abstract-factory
         * @m3.algorithm Abstract creation operation implemented by the selected factory.
         * @m3.dataStructures Returned A product reference.
         */
        A createPrimary();

        /**
         * Produces the secondary product of the family.
         * @m3.pattern gof:abstract-factory
         * @m3.algorithm Abstract creation operation implemented by the selected factory.
         * @m3.dataStructures Returned B product reference.
         */
        B createSecondary();
    }

    /**
     * Terminal construction contract for a configured builder.
     * @m3.pattern gof:builder
     * @m3.algorithm Leave configuration and assembly to an implementation of build.
     * @m3.dataStructures Generic result type T with no interface-owned storage.
     */
    @FunctionalInterface
    public interface Builder<T> {
        /**
         * Builds a result using implementation-defined configuration.
         * @m3.pattern gof:builder
         * @m3.algorithm Abstract assembly operation with no prescribed staging or allocation.
         * @m3.dataStructures Returned T result reference.
         */
        T build();
    }

    /**
     * Copying contract without a prescribed copy depth.
     * @m3.pattern gof:prototype
     * @m3.algorithm Defer object copying to the implementation of copy.
     * @m3.dataStructures Generic copied value type T and no interface-owned storage.
     */
    @FunctionalInterface
    public interface Prototype<T> {
        /**
         * Returns an implementation-defined copy without promising deep copying.
         * @m3.pattern gof:prototype
         * @m3.algorithm Abstract copy operation delegated to the concrete prototype.
         * @m3.dataStructures Returned T copy reference.
         */
        T copy();
    }

    /**
     * Lazily retains one successfully created non-null value per wrapper instance.
     * @m3.pattern gof:singleton
     * @m3.algorithm Use volatile double-checked locking, retrying after supplier failure or null result.
     * @m3.dataStructures Final Supplier reference and initially null volatile value reference.
     */
    public static final class Singleton<T> {
        /**
         * Construction callback retained for lazy initialization.
         * @m3.pattern gof:singleton
         * @m3.algorithm Assign the constructor's validated non-null supplier once.
         * @m3.dataStructures Final Supplier of T reference.
         */
        private final Supplier<T> supplier;
        /**
         * Published value, null until successful initialization.
         * @m3.pattern gof:singleton
         * @m3.algorithm Read through a volatile field and assign a non-null value inside the monitor.
         * @m3.dataStructures Volatile T reference initially null.
         */
        private volatile T value;

        /**
         * Stores the lazy construction callback without invoking it.
         * @m3.pattern gof:singleton
         * @m3.algorithm Reject a null supplier with Objects.requireNonNull and assign the field.
         * @m3.dataStructures Final Supplier reference and untouched null value field.
         */
        public Singleton(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "supplier");
        }

        /**
         * Returns the retained value, initializing it on the first successful request.
         * @m3.pattern gof:singleton
         * @m3.algorithm Read volatile value, recheck under this monitor, require a non-null supplier result and publish it.
         * @m3.dataStructures Local T reference, volatile value, Supplier and wrapper monitor.
         */
        public T get() {
            T current = value;
            if (current == null) {
                synchronized (this) {
                    current = value;
                    if (current == null) {
                        current = Objects.requireNonNull(supplier.get(), "singleton value");
                        value = current;
                    }
                }
            }
            return current;
        }

        /**
         * Reports whether a successful value has been published.
         * @m3.pattern gof:singleton
         * @m3.algorithm Compare the volatile value reference with null without invoking the supplier.
         * @m3.dataStructures Volatile T reference and boolean result.
         */
        public boolean initialized() {
            return value != null;
        }
    }

    // ---------------------------------------------------------------------
    // Structural (7)
    // ---------------------------------------------------------------------

    /**
     * Conversion contract between source and target representations.
     * @m3.pattern gof:adapter
     * @m3.algorithm Delegate conversion semantics to an implementation of adapt.
     * @m3.dataStructures Generic source S and target T types with no interface-owned storage.
     */
    @FunctionalInterface
    public interface Adapter<S, T> {
        /**
         * Converts the supplied source using the adapter implementation.
         * @m3.pattern gof:adapter
         * @m3.algorithm Abstract adaptation operation with no prescribed conversion algorithm.
         * @m3.dataStructures Input S reference and returned T reference.
         */
        T adapt(S source);
    }

    /**
     * Pairs an abstraction with its implementation without freezing either object.
     * @m3.pattern gof:bridge
     * @m3.algorithm Store two non-null references in a record and expose generated accessors.
     * @m3.dataStructures Record components A abstraction and I implementation.
     * @m3.member {@code FIELD:abstraction} pattern=gof:bridge; algorithm=Validate non-null in the compact constructor and retain the reference.; data.structures=Final record component of type A.; description=Abstraction reference associated with the implementation.
     * @m3.member {@code FIELD:implementation} pattern=gof:bridge; algorithm=Validate non-null in the compact constructor and retain the reference.; data.structures=Final record component of type I.; description=Implementation reference associated with the abstraction.
    */
    public record Bridge<A, I>(A abstraction, I implementation) {
        /**
         * Rejects null abstraction or implementation references.
         * @m3.pattern gof:bridge
         * @m3.algorithm Apply Objects.requireNonNull to both compact-constructor parameters.
         * @m3.dataStructures Two references assigned to generated record fields.
         */
        public Bridge {
            abstraction = Objects.requireNonNull(abstraction, "abstraction");
            implementation = Objects.requireNonNull(implementation, "implementation");
        }
    }

    /**
     * Common operation contract for leaf and composite participants.
     * @m3.pattern gof:composite
     * @m3.algorithm Delegate result production to an implementation of operation.
     * @m3.dataStructures Generic result type R with no interface-owned storage.
     */
    @FunctionalInterface
    public interface Component<R> {
        /**
         * Evaluates the component using its implementation.
         * @m3.pattern gof:composite
         * @m3.algorithm Abstract component operation without prescribed traversal or reduction.
         * @m3.dataStructures Returned R result reference.
         */
        R operation();
    }

    /**
     * Evaluates an ordered child list then reduces the child results.
     * @m3.pattern gof:composite
     * @m3.algorithm Take a shallow immutable list copy and apply the reducer to mapped child operations.
     * @m3.dataStructures Final List of Component references and Function from List of R to R.
     */
    public static final class Composite<R> implements Component<R> {
        /**
         * Immutable ordered list of child references, without copying child state.
         * @m3.pattern gof:composite
         * @m3.algorithm Initialize with List.copyOf in the constructor.
         * @m3.dataStructures Final List of Component references.
         */
        private final List<Component<R>> children;
        /**
         * Callback combining the ordered child results.
         * @m3.pattern gof:composite
         * @m3.algorithm Store a non-null constructor-supplied reduction function.
         * @m3.dataStructures Final Function from List of R to R.
         */
        private final Function<List<R>, R> reducer;

        /**
         * Snapshots the child list and stores the result reducer.
         * @m3.pattern gof:composite
         * @m3.algorithm Use List.copyOf and Objects.requireNonNull, rejecting null entries or reducer.
         * @m3.dataStructures Immutable List of child references and final reducer Function.
         */
        public Composite(List<? extends Component<R>> children, Function<List<R>, R> reducer) {
            this.children = List.copyOf(children);
            this.reducer = Objects.requireNonNull(reducer, "reducer");
        }

        /**
         * Returns the stored immutable list of child references.
         * @m3.pattern gof:composite
         * @m3.algorithm Return the children field directly without copying the children.
         * @m3.dataStructures Stored List of Component references.
         */
        public List<Component<R>> children() {
            return children;
        }

        /**
         * Runs child operations in list order and reduces their collected results.
         * @m3.pattern gof:composite
         * @m3.algorithm Map Component.operation over the sequential stream, collect toList and invoke reducer.apply.
         * @m3.dataStructures Stored children and reducer plus a newly collected result List.
         */
        @Override
        public R operation() {
            return reducer.apply(children.stream().map(Component::operation).toList());
        }
    }

    /**
     * Composes a delegate function with decoration of its output.
     * @m3.pattern gof:decorator
     * @m3.algorithm Apply the delegate first and then the unary decoration to its result.
     * @m3.dataStructures Final Function and UnaryOperator references.
     */
    public static final class Decorator<I, O> implements Function<I, O> {
        /**
         * Function producing the value to decorate.
         * @m3.pattern gof:decorator
         * @m3.algorithm Retain the constructor-validated non-null delegate.
         * @m3.dataStructures Final Function from I to O.
         */
        private final Function<I, O> delegate;
        /**
         * Unary function transforming the delegate result.
         * @m3.pattern gof:decorator
         * @m3.algorithm Retain the constructor-validated non-null decoration.
         * @m3.dataStructures Final UnaryOperator of O.
         */
        private final UnaryOperator<O> decoration;

        /**
         * Stores both functions after rejecting null callbacks.
         * @m3.pattern gof:decorator
         * @m3.algorithm Apply Objects.requireNonNull to delegate and decoration and assign both fields.
         * @m3.dataStructures Two final function references.
         */
        public Decorator(Function<I, O> delegate, UnaryOperator<O> decoration) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.decoration = Objects.requireNonNull(decoration, "decoration");
        }

        /**
         * Applies the delegate to the input, then decorates its returned result.
         *
         * @param input value passed to the delegate
         * @return the decorated result
         *
         * @m3.pattern gof:decorator
         * @m3.algorithm Sequential function composition: invoke the delegate,
         *     then invoke the decoration on the delegate's returned result
         * @m3.dataStructures Stored Function and UnaryOperator references,
         *     an input reference and an intermediate/output reference
         */
        @Override
        public O apply(I input) {
            return decoration.apply(delegate.apply(input));
        }
    }

    /**
     * Named supplier operations exposed through one lookup interface.
     * @m3.pattern gof:facade
     * @m3.algorithm Snapshot validated operations and dispatch call by exact String key.
     * @m3.dataStructures Immutable Map from String operation IDs to Supplier references.
     */
    public static final class Facade<R> {
        /**
         * Operation lookup snapshot without guaranteed map iteration order.
         * @m3.pattern gof:facade
         * @m3.algorithm Assign Map.copyOf of the constructor's validated temporary map.
         * @m3.dataStructures Final Map of String keys and Supplier references.
         */
        private final Map<String, Supplier<R>> operations;

        /**
         * Snapshots named operations after sorting and validating entries.
         * @m3.pattern gof:facade
         * @m3.algorithm Require a map, sort entries by key, reject null or blank IDs and null suppliers, then copy the map.
         * @m3.dataStructures Temporary LinkedHashMap and final immutable Map of suppliers.
         */
        public Facade(Map<String, ? extends Supplier<R>> operations) {
            Objects.requireNonNull(operations, "operations");
            LinkedHashMap<String, Supplier<R>> stable = new LinkedHashMap<>();
            operations.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    throw new IllegalArgumentException("facade operation id required");
                }
                stable.put(entry.getKey(), Objects.requireNonNull(entry.getValue(), "operation"));
            });
            this.operations = Map.copyOf(stable);
        }

        /**
         * Invokes the selected supplier or rejects an unknown operation ID.
         * @m3.pattern gof:facade
         * @m3.algorithm Look up the ID, throw if no supplier is found, otherwise call Supplier.get.
         * @m3.dataStructures Stored operation Map, String key and selected Supplier reference.
         */
        public R call(String operationId) {
            Supplier<R> operation = operations.get(operationId);
            if (operation == null) throw new IllegalArgumentException("unknown facade operation: " + operationId);
            return operation.get();
        }

        /**
         * Returns a sorted list of operation identifiers.
         * @m3.pattern gof:facade
         * @m3.algorithm Stream the map keys, sort them and collect with toList.
         * @m3.dataStructures Map key set and newly collected immutable List of Strings.
         */
        public List<String> operationIds() {
            return operations.keySet().stream().sorted().toList();
        }
    }

    /**
     * Shares non-null values by key in an unbounded concurrent pool.
     * @m3.pattern gof:flyweight
     * @m3.algorithm Create a value through computeIfAbsent only when the key has no mapping.
     * @m3.dataStructures ConcurrentHashMap of K to V and final factory Function.
     */
    public static final class Flyweight<K, V> {
        /**
         * Concurrent value pool with no eviction or configured capacity bound.
         * @m3.pattern gof:flyweight
         * @m3.algorithm Initialize a new ConcurrentHashMap for this wrapper.
         * @m3.dataStructures Final ConcurrentHashMap of keys and value references.
         */
        private final ConcurrentHashMap<K, V> pool = new ConcurrentHashMap<>();
        /**
         * Callback used to create absent keyed values.
         * @m3.pattern gof:flyweight
         * @m3.algorithm Retain the constructor-validated non-null factory.
         * @m3.dataStructures Final Function from K to V.
         */
        private final Function<K, V> factory;

        /**
         * Stores the value factory without populating the pool.
         * @m3.pattern gof:flyweight
         * @m3.algorithm Reject a null factory and assign the final field.
         * @m3.dataStructures Factory Function and newly initialized empty pool.
         */
        public Flyweight(Function<K, V> factory) {
            this.factory = Objects.requireNonNull(factory, "factory");
        }

        /**
         * Returns the pooled value or creates a non-null value for a non-null key.
         * @m3.pattern gof:flyweight
         * @m3.algorithm Require the key, then computeIfAbsent with a null-checked factory result.
         * @m3.dataStructures ConcurrentHashMap, key reference and returned value reference.
         */
        public V get(K key) {
            Objects.requireNonNull(key, "key");
            return pool.computeIfAbsent(key, value -> Objects.requireNonNull(factory.apply(value), "flyweight"));
        }

        /**
         * Reports the pool's current mapping count.
         * @m3.pattern gof:flyweight
         * @m3.algorithm Delegate to ConcurrentHashMap.size without freezing concurrent updates.
         * @m3.dataStructures Pool map and integer result.
         */
        public int size() {
            return pool.size();
        }
    }

    /**
     * Supplier proxy that materializes one successful target per instance.
     * @m3.pattern gof:proxy
     * @m3.algorithm Delegate lazy construction and publication to an owned Singleton wrapper.
     * @m3.dataStructures Final Singleton of T reference.
     */
    public static final class Proxy<T> implements Supplier<T> {
        /**
         * Owned lazy target wrapper.
         * @m3.pattern gof:proxy
         * @m3.algorithm Initialize a Singleton from the constructor's supplier.
         * @m3.dataStructures Final Singleton of T reference.
         */
        private final Singleton<T> target;

        /**
         * Creates a lazy target holder without calling the supplier.
         * @m3.pattern gof:proxy
         * @m3.algorithm Construct a Singleton with the supplied callback, inheriting its null check.
         * @m3.dataStructures New Singleton wrapper retaining the Supplier.
         */
        public Proxy(Supplier<T> supplier) {
            this.target = new Singleton<>(supplier);
        }

        /**
         * Returns the lazily materialized target.
         * @m3.pattern gof:proxy
         * @m3.algorithm Delegate directly to target.get.
         * @m3.dataStructures Stored Singleton and returned T reference.
         */
        @Override
        public T get() {
            return target.get();
        }

        /**
         * Reports whether target creation has succeeded.
         * @m3.pattern gof:proxy
         * @m3.algorithm Delegate directly to target.initialized.
         * @m3.dataStructures Stored Singleton and boolean result.
         */
        public boolean materialized() {
            return target.initialized();
        }
    }

    // ---------------------------------------------------------------------
    // Behavioral (11)
    // ---------------------------------------------------------------------

    /**
     * Request handler contract with optional acceptance.
     * @m3.pattern gof:chain-of-responsibility
     * @m3.algorithm Delegate request handling to the implementation returning an Optional.
     * @m3.dataStructures Input type I and Optional of output type O.
     */
    @FunctionalInterface
    public interface Handler<I, O> {
        /**
         * Handles a request or declines with an empty result.
         * @m3.pattern gof:chain-of-responsibility
         * @m3.algorithm Abstract handler operation whose Optional is checked by the chain caller.
         * @m3.dataStructures Input I reference and Optional of O result.
         */
        Optional<O> handle(I input);
    }

    /**
     * Ordered handler chain stopping at the first present result.
     * @m3.pattern gof:chain-of-responsibility
     * @m3.algorithm Snapshot handlers and scan until acceptance, rejecting null handler results.
     * @m3.dataStructures Immutable ordered List of Handler references and Optional results.
     */
    public static final class ChainOfResponsibility<I, O> {
        /**
         * Ordered immutable list of handlers, retaining their identities.
         * @m3.pattern gof:chain-of-responsibility
         * @m3.algorithm Initialize through List.copyOf without copying handler state.
         * @m3.dataStructures Final List of Handler references.
         */
        private final List<Handler<I, O>> handlers;

        /**
         * Snapshots the handler order and rejects null list entries.
         * @m3.pattern gof:chain-of-responsibility
         * @m3.algorithm Copy the supplied list with List.copyOf.
         * @m3.dataStructures Immutable List of Handler references.
         */
        public ChainOfResponsibility(List<? extends Handler<I, O>> handlers) {
            this.handlers = List.copyOf(handlers);
        }

        /**
         * Invokes handlers in order and returns the first present result, or an
         * empty result when every handler declines. Rejects a null handler result.
         *
         * @param input request passed to each handler
         * @return the first present handler result, or an empty result when all handlers decline
         *
         * @m3.pattern gof:chain-of-responsibility
         * @m3.algorithm Ordered linear scan with early return on the first
         *     present Optional, otherwise returning Optional.empty
         * @m3.dataStructures Immutable ordered List of Handler references
         *     and Optional result values
         */
        public Optional<O> handle(I input) {
            for (Handler<I, O> handler : handlers) {
                Optional<O> result = Objects.requireNonNull(handler.handle(input), "handler result");
                if (result.isPresent()) return result;
            }
            return Optional.empty();
        }
    }

    /**
     * Executable action contract with a result.
     * @m3.pattern gof:command
     * @m3.algorithm Delegate execution policy to the command implementation.
     * @m3.dataStructures Generic result type R and no interface-owned storage.
     */
    @FunctionalInterface
    public interface Command<R> {
        /**
         * Performs the command and returns its result.
         * @m3.pattern gof:command
         * @m3.algorithm Abstract execution operation without built-in queuing or undo.
         * @m3.dataStructures Returned R result reference.
         */
        R execute();
    }

    /**
     * Interpretation contract parameterized by context and result.
     * @m3.pattern gof:interpreter
     * @m3.algorithm Leave language and evaluation semantics to the implementation.
     * @m3.dataStructures Generic context C and result R types.
     */
    @FunctionalInterface
    public interface Interpreter<C, R> {
        /**
         * Interprets using the supplied context.
         * @m3.pattern gof:interpreter
         * @m3.algorithm Abstract evaluation operation without a built-in grammar or parser.
         * @m3.dataStructures Input C context reference and returned R reference.
         */
        R interpret(C context);
    }

    /**
     * Iterable snapshot of element references in their supplied order.
     * @m3.pattern gof:iterator
     * @m3.algorithm Take an immutable list copy and delegate iteration to that list.
     * @m3.dataStructures Final List of T references.
     */
    public static final class Sequence<T> implements Iterable<T> {
        /**
         * Immutable sequence snapshot without deep copying elements.
         * @m3.pattern gof:iterator
         * @m3.algorithm Initialize from List.copyOf of the supplied values.
         * @m3.dataStructures Final List of T references.
         */
        private final List<T> values;

        /**
         * Snapshots the ordered values and rejects null elements.
         * @m3.pattern gof:iterator
         * @m3.algorithm Copy the supplied list using List.copyOf.
         * @m3.dataStructures Immutable List of element references.
         */
        public Sequence(List<T> values) {
            this.values = List.copyOf(values);
        }

        /**
         * Returns an iterator over the stored sequence snapshot.
         * @m3.pattern gof:iterator
         * @m3.algorithm Delegate directly to values.iterator.
         * @m3.dataStructures Stored immutable List and newly obtained Iterator of T.
         */
        @Override
        public Iterator<T> iterator() {
            return values.iterator();
        }

        /**
         * Returns the number of stored elements.
         * @m3.pattern gof:iterator
         * @m3.algorithm Delegate directly to values.size.
         * @m3.dataStructures Stored List and integer result.
         */
        public int size() {
            return values.size();
        }
    }

    /**
     * Routes messages synchronously to uniquely named participants.
     * @m3.pattern gof:mediator
     * @m3.algorithm Register consumers with putIfAbsent and dispatch each send to one consumer.
     * @m3.dataStructures ConcurrentHashMap viewed as Map from String to Consumer of M.
     */
    public static final class Mediator<M> {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public Mediator() {
        }

        /**
         * Concurrent registry of participant callbacks.
         * @m3.pattern gof:mediator
         * @m3.algorithm Initialize an empty ConcurrentHashMap for registration and lookup.
         * @m3.dataStructures Final Map of String IDs to Consumer references.
         */
        private final Map<String, java.util.function.Consumer<M>> participants = new ConcurrentHashMap<>();

        /**
         * Registers a non-null receiver under a nonblank unique ID.
         * @m3.pattern gof:mediator
         * @m3.algorithm Validate ID, null-check receiver, putIfAbsent and reject an existing mapping.
         * @m3.dataStructures Registry Map, String ID and Consumer reference.
         */
        public void register(String id, java.util.function.Consumer<M> receiver) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("participant id required");
            java.util.function.Consumer<M> prior =
                    participants.putIfAbsent(id, Objects.requireNonNull(receiver, "receiver"));
            if (prior != null) throw new IllegalArgumentException("duplicate mediator participant: " + id);
        }

        /**
         * Invokes the named participant synchronously or rejects an unknown ID.
         * @m3.pattern gof:mediator
         * @m3.algorithm Look up the receiver, throw when absent, then call Consumer.accept.
         * @m3.dataStructures Registry Map, target String and message M reference.
         */
        public void send(String targetId, M message) {
            java.util.function.Consumer<M> receiver = participants.get(targetId);
            if (receiver == null) throw new IllegalArgumentException("unknown mediator participant: " + targetId);
            receiver.accept(message);
        }
    }

    /**
     * Record of a state reference that may be null or mutable.
     * @m3.pattern gof:memento
     * @m3.algorithm Retain the supplied state without copying its object graph.
     * @m3.dataStructures Single generated final record field of type T.
     * @m3.member {@code FIELD:state} pattern=gof:memento; algorithm=Store the record constructor argument directly, including null.; data.structures=Final record component of type T.; description=Saved state reference without a deep snapshot guarantee.
    */
    public record Memento<T>(T state) {}

    /**
     * Unsynchronized ordered history of memento wrappers.
     * @m3.pattern gof:memento
     * @m3.algorithm Append a wrapper per save and expose latest or indexed retrieval.
     * @m3.dataStructures ArrayList viewed as List of Memento references.
     */
    public static final class Caretaker<T> {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public Caretaker() {
        }

        /**
         * Mutable history owned by the caretaker without synchronization.
         * @m3.pattern gof:memento
         * @m3.algorithm Initialize an empty ArrayList and append on save.
         * @m3.dataStructures Final List of Memento references backed by ArrayList.
         */
        private final List<Memento<T>> history = new ArrayList<>();

        /**
         * Appends a memento for the supplied state, including null.
         * @m3.pattern gof:memento
         * @m3.algorithm Construct a Memento and add it to the history list.
         * @m3.dataStructures New Memento retaining T and mutable history List.
         */
        public void save(T state) {
            history.add(new Memento<>(state));
        }

        /**
         * Returns the latest saved wrapper or rejects an empty history.
         * @m3.pattern gof:memento
         * @m3.algorithm Check isEmpty, throw if empty, otherwise call getLast.
         * @m3.dataStructures History List and returned Memento reference.
         */
        public Memento<T> latest() {
            if (history.isEmpty()) throw new IllegalStateException("no memento available");
            return history.getLast();
        }

        /**
         * Returns the saved wrapper at the requested index.
         * @m3.pattern gof:memento
         * @m3.algorithm Delegate to List.get, inheriting its index bounds checks.
         * @m3.dataStructures History List, integer index and returned Memento.
         */
        public Memento<T> get(int index) {
            return history.get(index);
        }

        /**
         * Returns the number of saved wrappers.
         * @m3.pattern gof:memento
         * @m3.algorithm Delegate directly to history.size.
         * @m3.dataStructures History List and integer result.
         */
        public int size() {
            return history.size();
        }
    }

    /**
     * Synchronous event publisher with copy-on-write observer storage.
     * @m3.pattern gof:observer
     * @m3.algorithm Append subscriptions and traverse a list snapshot on publish.
     * @m3.dataStructures CopyOnWriteArrayList of Consumer references.
     */
    public static final class Subject<E> {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public Subject() {
        }

        /**
         * Observer list allowing duplicate callback references.
         * @m3.pattern gof:observer
         * @m3.algorithm Initialize an empty CopyOnWriteArrayList for subscription mutation and snapshot traversal.
         * @m3.dataStructures Final CopyOnWriteArrayList of Consumer of E.
         */
        private final CopyOnWriteArrayList<java.util.function.Consumer<E>> observers =
                new CopyOnWriteArrayList<>();

        /**
         * Adds a non-null observer and returns a handle removing an equal occurrence.
         * @m3.pattern gof:observer
         * @m3.algorithm Null-check and append the consumer, then return a lambda calling remove on each close.
         * @m3.dataStructures Observer list, Consumer reference and AutoCloseable lambda.
         */
        public AutoCloseable subscribe(java.util.function.Consumer<E> observer) {
            Objects.requireNonNull(observer, "observer");
            observers.add(observer);
            return () -> observers.remove(observer);
        }

        /**
         * Calls observers synchronously in snapshot order until completion or exception.
         * @m3.pattern gof:observer
         * @m3.algorithm Use CopyOnWriteArrayList.forEach to invoke accept with the event, propagating callback failures.
         * @m3.dataStructures Observer snapshot and shared E event reference.
         */
        public void publish(E event) {
            observers.forEach(observer -> observer.accept(event));
        }

        /**
         * Reports the current number of observer entries including duplicates.
         * @m3.pattern gof:observer
         * @m3.algorithm Delegate directly to observers.size.
         * @m3.dataStructures Copy-on-write observer list and integer result.
         */
        public int observerCount() {
            return observers.size();
        }
    }

    /**
     * Non-null state and event pair used for transition map lookup.
     * @m3.pattern gof:state
     * @m3.algorithm Validate both components and use generated record equality and hashing.
     * @m3.dataStructures Two final record components S state and E event.
     * @m3.member {@code FIELD:state} pattern=gof:state; algorithm=Validate non-null in the compact constructor and retain the reference.; data.structures=Final record component of type S.; description=State reference forming the first part of a transition key.
     * @m3.member {@code FIELD:event} pattern=gof:state; algorithm=Validate non-null in the compact constructor and retain the reference.; data.structures=Final record component of type E.; description=Event reference forming the second part of a transition key.
    */
    public record TransitionKey<S, E>(S state, E event) {
        /**
         * Rejects null state or event key components.
         * @m3.pattern gof:state
         * @m3.algorithm Apply Objects.requireNonNull to both constructor parameters.
         * @m3.dataStructures Two references assigned to generated record fields.
         */
        public TransitionKey {
            state = Objects.requireNonNull(state, "state");
            event = Objects.requireNonNull(event, "event");
        }
    }

    /**
     * Synchronized state holder driven by an immutable transition map snapshot.
     * @m3.pattern gof:state
     * @m3.algorithm Look up the current state and event pair before assigning the next state.
     * @m3.dataStructures Final Map from TransitionKey to S and mutable S state field.
     */
    public static final class StateMachine<S, E> {
        /**
         * Immutable transition map structure retaining key and state references.
         * @m3.pattern gof:state
         * @m3.algorithm Initialize with Map.copyOf in the constructor.
         * @m3.dataStructures Final Map from TransitionKey of S and E to S.
         */
        private final Map<TransitionKey<S, E>, S> transitions;
        /**
         * Current state guarded by synchronized access after construction.
         * @m3.pattern gof:state
         * @m3.algorithm Initialize from the non-null starting state and replace only after successful lookup.
         * @m3.dataStructures Mutable S reference protected by the instance monitor.
         */
        private S state;

        /**
         * Stores a non-null initial state and snapshots the transition map.
         * @m3.pattern gof:state
         * @m3.algorithm Require the initial state and copy transitions without requiring an outgoing initial-state entry.
         * @m3.dataStructures Initial S reference and immutable Map of transition keys to states.
         */
        public StateMachine(S initialState, Map<TransitionKey<S, E>, S> transitions) {
            this.state = Objects.requireNonNull(initialState, "initialState");
            this.transitions = Map.copyOf(transitions);
        }

        /**
         * Returns the current state under the instance monitor.
         * @m3.pattern gof:state
         * @m3.algorithm Enter the synchronized method and read the state field.
         * @m3.dataStructures Instance monitor and stored S reference.
         */
        public synchronized S state() {
            return state;
        }

        /**
         * Applies a defined transition or leaves state unchanged when none exists.
         * @m3.pattern gof:state
         * @m3.algorithm Under the monitor construct a non-null key, look up next state, reject absence, then assign and return it.
         * @m3.dataStructures Instance monitor, temporary TransitionKey, transition Map and S references.
         */
        public synchronized S accept(E event) {
            TransitionKey<S, E> key = new TransitionKey<>(state, event);
            S next = transitions.get(key);
            if (next == null) throw new IllegalStateException("no state transition for " + key);
            state = next;
            return state;
        }
    }

    /**
     * Interchangeable input-to-output algorithm contract.
     * @m3.pattern gof:strategy
     * @m3.algorithm Delegate the transformation to the selected implementation of apply.
     * @m3.dataStructures Generic input I and output O types without interface-owned state.
     */
    @FunctionalInterface
    public interface Strategy<I, O> {
        /**
         * Transforms input using the selected strategy implementation.
         * @m3.pattern gof:strategy
         * @m3.algorithm Abstract operation with algorithm and null policy left to the implementation.
         * @m3.dataStructures Input I reference and returned O reference.
         */
        O apply(I input);
    }

    /**
     * Context that delegates inputs to a replaceable, non-null strategy.
     * Publishing the strategy reference does not make the strategy implementation thread-safe.
     *
     * @param <I> input type accepted by the selected strategy
     * @param <O> output type returned by the selected strategy
     * @m3.pattern gof:strategy
     * @m3.algorithm Null-check-and-store initialization and replacement, with
     *     direct delegation to the selected strategy for execution
     * @m3.dataStructures One volatile Strategy reference; input and output values
     *     are passed to and returned from the selected strategy
     */
    public static final class StrategyContext<I, O> {
        /**
         * Selected non-null strategy reference published through a volatile field.
         * @m3.pattern gof:strategy
         * @m3.algorithm Assign on construction or validated replacement without making strategy internals thread-safe.
         * @m3.dataStructures Volatile Strategy of I and O reference.
         */
        private volatile Strategy<I, O> strategy;

        /**
         * Creates a context using the supplied non-null strategy.
         *
         * @param strategy initial strategy selected for execution
         * @throws NullPointerException if strategy is null
         * @m3.pattern gof:strategy
         * @m3.algorithm Reject a null strategy with Objects.requireNonNull, then
         *     assign the accepted reference to the strategy field
         * @m3.dataStructures The supplied Strategy reference and the context's
         *     volatile strategy field
         */
        public StrategyContext(Strategy<I, O> strategy) {
            this.strategy = Objects.requireNonNull(strategy, "strategy");
        }

        /**
         * Passes the input to the selected strategy and returns its result.
         *
         * @param input value passed unchanged to the selected strategy
         * @return the selected strategy's result, which may be null
         * @m3.pattern gof:strategy
         * @m3.algorithm Read the stored strategy reference and invoke its apply
         *     method once with the input, returning that invocation's result
         * @m3.dataStructures One stored Strategy reference, an input value and
         *     the returned output value
         */
        public O execute(I input) {
            return strategy.apply(input);
        }

        /**
         * Replaces the selected strategy after rejecting null.
         *
         * @param replacement strategy selected for subsequent execution
         * @throws NullPointerException if replacement is null
         * @m3.pattern gof:strategy
         * @m3.algorithm Validate the replacement with Objects.requireNonNull,
         *     then assign it to the volatile strategy field
         * @m3.dataStructures The replacement Strategy reference and the
         *     context's volatile strategy field
         */
        public void strategy(Strategy<I, O> replacement) {
            strategy = Objects.requireNonNull(replacement, "replacement");
        }
    }

    /**
     * Fixed execution skeleton with overridable preparation, core and finishing hooks.
     * @m3.pattern gof:template-method
     * @m3.algorithm Invoke before, core and after in order, propagating any hook failure.
     * @m3.dataStructures Input I and intermediate or final O references with no owned fields.
     */
    public abstract static class TemplateMethod<I, O> {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public TemplateMethod() {
        }

        /**
         * Executes the three hooks in order and returns the final output.
         * @m3.pattern gof:template-method
         * @m3.algorithm Call before(input), core(prepared) and after(result), stopping on an exception.
         * @m3.dataStructures Local prepared I and result O references.
         */
        public final O execute(I input) {
            I prepared = before(input);
            O result = core(prepared);
            return after(result);
        }

        /**
         * Default preparation hook preserving the supplied input.
         * @m3.pattern gof:template-method
         * @m3.algorithm Return the input reference unchanged.
         * @m3.dataStructures Input and returned I reference.
         */
        protected I before(I input) {
            return input;
        }

        /**
         * Required core computation of the template.
         * @m3.pattern gof:template-method
         * @m3.algorithm Abstract hook implemented by a subclass, called after preparation.
         * @m3.dataStructures Prepared I reference and returned O reference.
         */
        protected abstract O core(I input);

        /**
         * Default finishing hook preserving the core result.
         * @m3.pattern gof:template-method
         * @m3.algorithm Return the output reference unchanged.
         * @m3.dataStructures Input and returned O reference.
         */
        protected O after(O output) {
            return output;
        }
    }

    /**
     * Operation contract applied to a visited value.
     * @m3.pattern gof:visitor
     * @m3.algorithm Leave traversal and evaluation to the visit implementation.
     * @m3.dataStructures Generic visited N and result R types.
     */
    @FunctionalInterface
    public interface Visitor<N, R> {
        /**
         * Processes the supplied node value.
         * @m3.pattern gof:visitor
         * @m3.algorithm Abstract visit operation without built-in graph traversal.
         * @m3.dataStructures Input N reference and returned R reference.
         */
        R visit(N node);
    }

    /**
     * Wraps a non-null value for application of visitor callbacks.
     * @m3.pattern gof:visitor
     * @m3.algorithm Retain the value and pass it directly to the chosen visitor.
     * @m3.dataStructures Final N value reference.
     */
    public static final class Visitable<N> {
        /**
         * Non-null visited object reference without deep immutability.
         * @m3.pattern gof:visitor
         * @m3.algorithm Retain the constructor-validated value.
         * @m3.dataStructures Final N reference.
         */
        private final N value;

        /**
         * Stores the wrapped value after rejecting null.
         * @m3.pattern gof:visitor
         * @m3.algorithm Apply Objects.requireNonNull and assign the value field.
         * @m3.dataStructures Final N reference.
         */
        public Visitable(N value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        /**
         * Invokes a non-null visitor on the stored value.
         * @m3.pattern gof:visitor
         * @m3.algorithm Require the visitor and return visitor.visit(value).
         * @m3.dataStructures Stored N reference, Visitor callback and R result.
         */
        public <R> R accept(Visitor<N, R> visitor) {
            return Objects.requireNonNull(visitor, "visitor").visit(value);
        }

        /**
         * Returns the wrapped value reference.
         * @m3.pattern gof:visitor
         * @m3.algorithm Return the value field directly without copying.
         * @m3.dataStructures Stored N reference.
         */
        public N value() {
            return value;
        }
    }
}
