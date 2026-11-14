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
package jdk.internal.m3.paradigm.context;

import java.time.Instant;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Listener-free context handle. SQLite is reached through the trusted host's storage binding,
 * never through a listener. No database connection, task or subscriber list lives in this object.
 * The codec and policy must be deterministic and safe for concurrent use. External effects belong
 * outside mutation/validation lambdas; an optimistic conflict never grants permission to retry them.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/stream/DurableContext.java}
 * blob {@code cb686a774d1b032a5d359fa178d5de171207aa7e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DurableContext<T> {
    /** Query marker distinct from a present JSON null. */
    public enum Missing { VALUE }
    public enum Action { READ, CREATE, APPLY, RESTORE, INDEX, ACKNOWLEDGE }
    public record Document(String schema, int version, String json) {
        public Document {
            schema = token(schema); Objects.requireNonNull(json);
            if (version < 1) throw new IllegalArgumentException("Invalid schema version");
        }
    }
    public record Version(UUID uid, long revision, Instant recordedAt, Document document) {
        public Version {
            Objects.requireNonNull(uid); Objects.requireNonNull(recordedAt); Objects.requireNonNull(document);
            if (revision < 0) throw new IllegalArgumentException("Negative revision");
        }
    }
    public record Command(UUID id, String actor, String operation, long expectedRevision, Long restoredFrom) {
        public Command {
            Objects.requireNonNull(id); actor = token(actor); operation = token(operation);
            if (expectedRevision < -1 || restoredFrom != null && restoredFrom < 0)
                throw new IllegalArgumentException("Invalid revision");
        }
        public static Command apply(UUID id, String actor, String operation, long expected) {
            return new Command(id, actor, operation, expected, null);
        }
    }
    public record Receipt(Command command, Version version, boolean replayed) {
        public Receipt { Objects.requireNonNull(command); Objects.requireNonNull(version); }
    }
    public record Delivery(long sequence, long revision, UUID eventId, String topic, String json) {
        public Delivery {
            if (sequence < 1 || revision < 0) throw new IllegalArgumentException("Invalid delivery cursor");
            Objects.requireNonNull(eventId); Objects.requireNonNull(topic); Objects.requireNonNull(json);
        }
    }
    public record Access(UUID uid, Action action, String actor, String operation, long expectedRevision) { }
    public record Transition<T>(Access access, T before, T after) { }
    public record Contract<T>(String id, Predicate<T> invariant, Predicate<Access> authority,
                              Predicate<Transition<T>> transition) {
        public Contract {
            id = token(id); Objects.requireNonNull(invariant); Objects.requireNonNull(authority);
            Objects.requireNonNull(transition);
        }
        public static <T> Contract<T> local(String versionedId, Predicate<T> invariant) {
            return new Contract<>(versionedId, invariant, access -> true, change -> true);
        }
    }
    public record Format<T>(String schema, int version, Function<T, String> encode, Function<String, T> decode) {
        public Format {
            schema = token(schema); if (version < 1) throw new IllegalArgumentException("Invalid format version");
            Objects.requireNonNull(encode); Objects.requireNonNull(decode);
        }
    }

    /**
     * Trusted host SPI, not accessible through a context handle. The production Grail binding uses
     * its existing outbox to commit the candidate, request identity and event in one transaction.
     * Implementations must reject stale heads and mismatched reuse of command IDs atomically.
     */
    public interface Store {
        UUID uid();
        Optional<Version> latest() throws Exception;
        Optional<Version> read(long revision) throws Exception;
        Receipt commit(Command command, Document document) throws Exception;
        List<Version> history(long afterExclusive, long throughRevision, int limit) throws Exception;
        List<Delivery> replay(long afterSequence, long throughRevision, int limit) throws Exception;
        List<Version> query(String property, Object value, long throughRevision, int limit) throws Exception;
        void index(String property) throws Exception;
        long cursor(String consumer) throws Exception;
        void acknowledge(String consumer, long expected, long through) throws Exception;
    }

    private final Store store;
    private final Format<T> format;
    private final Contract<T> contract;
    private final String actor;
    private final Set<Action> allowed;
    private final Version pinned;

    /** Host-only construction; ordinary callers use the UID repository factory. */
    public DurableContext(Store store, Format<T> format, Contract<T> contract, String actor) {
        this(store, format, contract, actor, EnumSet.allOf(Action.class), null);
    }
    private DurableContext(Store store, Format<T> format, Contract<T> contract, String actor,
                           Set<Action> allowed, Version pinned) {
        this.store = Objects.requireNonNull(store); this.format = Objects.requireNonNull(format);
        this.contract = Objects.requireNonNull(contract); this.actor = token(actor);
        this.allowed = Set.copyOf(allowed); this.pinned = pinned;
    }
    public UUID uid() { return store.uid(); }
    public boolean frozen() { return pinned != null; }
    public DurableContext<T> readOnly() { return restrict(Set.of(Action.READ)); }
    public DurableContext<T> restrict(Set<Action> actions) {
        Set<Action> copy = Set.copyOf(actions);
        return new DurableContext<>(store, format, contract, actor,
                allowed.stream().filter(copy::contains).collect(Collectors.toUnmodifiableSet()), pinned);
    }
    /** Capture once; frozen snapshot/visit reads need no database access and remain data-only after scope close. */
    public Flow.Publisher<DurableContext<T>> freeze() {
        return Flows.once(() -> {
            authorize(Action.READ, "freeze", -1);
            Version snapshot = current();
            return new DurableContext<>(store, format, contract, actor, Set.of(Action.READ), snapshot);
        });
    }
    public Flow.Publisher<Version> snapshot() {
        return Flows.once(() -> { authorize(Action.READ, "snapshot", -1); return current(); });
    }
    /** Coherent read-only projection. No command/event/history write is hidden in a visit. */
    public <R> Flow.Publisher<R> visit(Function<? super T, ? extends R> visitor) {
        Objects.requireNonNull(visitor);
        return Flows.once(() -> {
            authorize(Action.READ, "visit", -1);
            return Objects.requireNonNull(visitor.apply(decode(current())), "visitor result");
        });
    }
    public Flow.Publisher<Receipt> create(UUID commandId, T initial) {
        Objects.requireNonNull(initial);
        // Encode immediately so later caller mutation cannot change this command's input.
        Document document = encode(initial);
        Command command = new Command(commandId, actor, "CREATE", -1, null);
        return Flows.once(() -> {
            Access access = authorize(Action.CREATE, command.operation(), -1);
            T after = decode(document);
            validate(access, null, after, document);
            return store.commit(command, document);
        });
    }
    public Flow.Publisher<Receipt> apply(UUID commandId, long expectedRevision, String operation, UnaryOperator<T> edit) {
        if (expectedRevision < 0) throw new IllegalArgumentException("Use create for the initial revision");
        Objects.requireNonNull(edit);
        Command command = Command.apply(commandId, actor, operation, expectedRevision);
        return Flows.once(() -> {
            Access access = authorize(Action.APPLY, command.operation(), expectedRevision);
            Version previous = required(expectedRevision);
            T before = decode(previous);
            // Separate decoding prevents an in-place edit from mutating the 'before' contract input.
            Document candidate = encode(Objects.requireNonNull(edit.apply(decode(previous)), "candidate"));
            validate(access, before, decode(candidate), candidate);
            return store.commit(command, candidate);
        });
    }
    /** Trusted pre-bound handle, normalized once. It runs through exactly the same guards and commit path. */
    @SuppressWarnings("unchecked")
    public Flow.Publisher<Receipt> apply(UUID commandId, long expectedRevision, String operation, MethodHandle handle) {
        MethodHandle bound = Objects.requireNonNull(handle).asType(MethodType.methodType(Object.class, Object.class));
        return apply(commandId, expectedRevision, operation, value -> {
            try { return (T) (Object) bound.invokeExact((Object) value); }
            catch (RuntimeException | Error failure) { throw failure; }
            catch (Throwable failure) { throw new IllegalStateException("Context handle failed", failure); }
        });
    }
    /** Restored DATA is checked by today's bound contract and appended as a new revision. */
    public Flow.Publisher<Receipt> restore(UUID commandId, long expectedRevision, long sourceRevision) {
        if (expectedRevision < 0 || sourceRevision < 0) throw new IllegalArgumentException("Invalid restore revision");
        Command command = new Command(commandId, actor, "RESTORE", expectedRevision, sourceRevision);
        return Flows.once(() -> {
            Access access = authorize(Action.RESTORE, "RESTORE", expectedRevision);
            T before = decode(required(expectedRevision));
            Document candidate = encode(decode(required(sourceRevision)));
            validate(access, before, decode(candidate), candidate);
            return store.commit(command, candidate);
        });
    }
    public Flow.Publisher<Version> history(long afterExclusive, int limit) {
        page(limit); if (afterExclusive < -1) throw new IllegalArgumentException("Invalid history cursor");
        return Flows.page(() -> {
            authorize(Action.READ, "history", -1);
            return checked(store.history(afterExclusive, through(), limit));
        });
    }
    public Flow.Publisher<Version> query(String property, Object value, int limit) {
        page(limit); String key = token(property);
        return Flows.page(() -> {
            authorize(Action.READ, "query", -1);
            return checked(store.query(key, value, through(), limit));
        });
    }
    public Flow.Publisher<String> index(String property) {
        String key = token(property);
        return Flows.once(() -> { authorize(Action.INDEX, "index", -1); store.index(key); return key; });
    }
    public Flow.Publisher<Delivery> events(long afterSequence, int limit) {
        page(limit); if (afterSequence < 0) throw new IllegalArgumentException("Invalid event cursor");
        return Flows.page(() -> {
            authorize(Action.READ, "events", -1);
            return store.replay(afterSequence, through(), limit);
        });
    }
    public Flow.Publisher<Delivery> follow(long afterSequence) {
        return Flows.follow(afterSequence, after -> {
            authorize(Action.READ, "follow", -1);
            if (pinned != null) throw new IllegalStateException("Frozen view cannot follow future events");
            try { return store.replay(after, Long.MAX_VALUE, 1).stream().findFirst(); }
            catch (RuntimeException failure) { throw failure; }
            catch (Exception failure) { throw new IllegalStateException("Replay failed", failure); }
        }, Delivery::sequence);
    }
    public Flow.Publisher<Long> cursor(String consumer) {
        String name = token(consumer);
        return Flows.once(() -> { authorize(Action.READ, "cursor", -1); return store.cursor(name); });
    }
    public Flow.Publisher<Long> acknowledge(String consumer, long expected, long throughSequence) {
        String name = token(consumer);
        if (expected < 0 || throughSequence <= expected) throw new IllegalArgumentException("Cursor must advance");
        return Flows.once(() -> {
            authorize(Action.ACKNOWLEDGE, "acknowledge", -1);
            store.acknowledge(name, expected, throughSequence); return throughSequence;
        });
    }

    private Access authorize(Action action, String operation, long expected) {
        if (!allowed.contains(action) || pinned != null && action != Action.READ) throw new SecurityException("Restricted context view");
        Access access = new Access(uid(), action, actor, operation, expected);
        if (!contract.authority().test(access)) throw new SecurityException("Context authority denied operation");
        return access;
    }
    private void validate(Access access, T before, T after, Document candidate) {
        Document beforeImage = before == null ? null : encode(before);
        Document afterImage = encode(after);
        if (!afterImage.equals(candidate)) throw new IllegalArgumentException("Codec must round-trip the complete candidate deterministically");
        if (!contract.invariant().test(after)) throw new IllegalArgumentException("Context invariant rejected candidate");
        if (!contract.transition().test(new Transition<>(access, before, after))) throw new IllegalArgumentException("Context contract rejected transition");
        if (!afterImage.equals(encode(after)) || before != null && !beforeImage.equals(encode(before)))
            throw new IllegalArgumentException("Context guards must not mutate their inputs");
    }
    private Version current() throws Exception {
        Version value = pinned == null ? store.latest().orElseThrow(() -> new IllegalStateException("Context has no memento")) : pinned;
        check(value); return value;
    }
    private Version required(long revision) throws Exception {
        Version value = store.read(revision).orElseThrow(() -> new IllegalArgumentException("Missing context revision " + revision));
        check(value);
        if (value.revision() != revision) throw new IllegalStateException("Store returned wrong revision");
        return value;
    }
    private List<Version> checked(List<Version> versions) { versions.forEach(this::check); return List.copyOf(versions); }
    private void check(Version version) {
        if (!version.uid().equals(uid()) || version.revision() > through()) throw new IllegalStateException("Foreign or future snapshot");
        Document doc = version.document();
        if (!doc.schema().equals(format.schema()) || doc.version() != format.version()) throw new IllegalArgumentException("Explicit codec migration required");
    }
    private Document encode(T value) { return new Document(format.schema(), format.version(), Objects.requireNonNull(format.encode().apply(value))); }
    private T decode(Version value) { check(value); return decode(value.document()); }
    private T decode(Document doc) { return Objects.requireNonNull(format.decode().apply(doc.json()), "decoded value"); }
    private long through() { return pinned == null ? Long.MAX_VALUE : pinned.revision(); }
    private static void page(int limit) { if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Page size must be 1..1000"); }
    private static String token(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank() || value.length() > 200 || value.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid context token");
        JsonText.quote(value); // Validate Unicode before using identifiers in SQL metadata.
        return value;
    }
}
