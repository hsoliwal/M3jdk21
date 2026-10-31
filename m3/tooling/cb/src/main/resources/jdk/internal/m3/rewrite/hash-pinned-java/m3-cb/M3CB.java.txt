// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Caller-owned runtime bridge over an existing class snapshot, with no global live-class map.
 * The existing boundary weakly owns mirrors/loaders. Only immutable identity/hierarchy queries
 * are accelerated. Any unsupported lifecycle event must invalidate the session before reuse.
 * Declaration of an artifact association never certifies actual transformed definition bytes.
 */
public final class M3CB implements AutoCloseable {
    public enum Mode { OFF, OBSERVE, ENABLED }

    /** Caller-visible projection; never retained as one structural object per row. */
    public record DeclaredAttachment(
            M3CB session, M3Class type, M3Release release,
            M3PC candidateKey, long epoch) {
        public DeclaredAttachment {
            Objects.requireNonNull(session, "session"); Objects.requireNonNull(type, "type");
            Objects.requireNonNull(release, "release"); Objects.requireNonNull(candidateKey, "candidateKey");
        }
    }

    public record Statistics(long reused, long observed, long fallback, long mismatches) {}

    private final M3CI index;
    private final Mode mode;
    private final M3Release[] releases;
    private final M3PC[] keys;
    private long epoch;
    private long reused;
    private long observed;
    private long fallback;
    private long mismatches;
    private boolean valid = true;
    private boolean closed;

    private M3CB(M3CI index, Mode mode) {
        this.index = Objects.requireNonNull(index, "index");
        this.mode = Objects.requireNonNull(mode, "mode");
        releases = new M3Release[index.size()];
        keys = new M3PC[index.size()];
    }

    /** Reuses prepared metadata without reflection or rebuilding it. Compact snapshots fall back. */
    public static M3CB over(M3CI index, Mode mode) {
        return new M3CB(index, mode);
    }

    public static M3CB prepare(
            Collection<Class<?>> roots, M3CI.Options options,
            Mode mode, M3VI.Progress monitor) {
        Objects.requireNonNull(mode, "mode");
        return over(M3CI.compile(roots, options, monitor), mode);
    }

    public synchronized long epoch() { return epoch; }

    public synchronized Optional<M3Class> fromClass(Class<?> type) {
        requireOpen();
        Objects.requireNonNull(type, "type");
        return valid ? index.fromClass(type) : Optional.empty();
    }

    public synchronized String name(Class<?> type) {
        requireOpen();
        Objects.requireNonNull(type, "type");
        Optional<M3Class> prepared = mode == Mode.OFF || !valid
                ? Optional.empty() : index.fromClass(type);
        if (prepared.isEmpty()) {
            fallback++;
            return type.getName();
        }
        String candidate = prepared.orElseThrow().name();
        if (mode == Mode.OBSERVE) {
            observed++;
            String authoritative = type.getName();
            if (!candidate.equals(authoritative)) { mismatches++; invalidate(); }
            return authoritative;
        }
        reused++;
        return candidate;
    }

    public synchronized boolean isAssignableFrom(Class<?> target, Class<?> candidate) {
        requireOpen();
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(candidate, "candidate");
        Optional<M3Class> left = mode == Mode.OFF || !valid
                ? Optional.empty() : index.fromClass(target);
        Optional<M3Class> right = left.isEmpty() ? Optional.empty() : index.fromClass(candidate);
        if (left.isEmpty() || right.isEmpty()) {
            fallback++;
            return target.isAssignableFrom(candidate);
        }
        boolean result = index.isAssignableFrom(left.orElseThrow().row(), right.orElseThrow().row());
        if (mode == Mode.OBSERVE) {
            observed++;
            boolean authoritative = target.isAssignableFrom(candidate);
            if (result != authoritative) { mismatches++; invalidate(); }
            return authoritative;
        }
        reused++;
        return result;
    }

    /**
     * Atomically publishes a declaration for this actual Class, not for the requesting loader.
     * Repeating the same declaration is idempotent. Conflicts and old epochs are rejected.
     * This API is deliberately named declare: it cannot authenticate a supplied byte digest.
     */
    public synchronized DeclaredAttachment declare(
            Class<?> type, M3Release release, M3PC key,
            long expectedEpoch) {
        requireOpen();
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(key, "key");
        if (!valid || expectedEpoch != epoch) throw new IllegalStateException("stale session");
        M3Class view = index.fromClass(Objects.requireNonNull(type, "type"))
                .orElseThrow(() -> new IllegalArgumentException("class outside live snapshot"));
        int row = view.row();
        if (releases[row] != null
                && (!releases[row].equals(release) || !keys[row].sameInputs(key))) {
            throw new IllegalStateException("conflicting class declaration");
        }
        if (releases[row] == null) { releases[row] = release; keys[row] = key; }
        return new DeclaredAttachment(this, view, releases[row], keys[row], epoch);
    }

    /**
     * Effective declared release of this actual Class, derived from its indexed counterpart row.
     * This is provenance lookup, not definition authentication or permission to replace bytecode.
     * The caller must invalidate before redefinition/retransformation; a retired bridge returns empty.
     */
    public synchronized Optional<M3Release> effectiveVersion(Class<?> type) {
        return effective(type).map(DeclaredAttachment::release);
    }

    /** Current counterpart provenance; projections are caller-owned and not cached per row. */
    public synchronized Optional<DeclaredAttachment> effective(Class<?> type) {
        requireOpen(); Objects.requireNonNull(type, "type");
        if (!valid) return Optional.empty();
        Optional<M3Class> view = index.fromClass(type);
        if (view.isEmpty()) return Optional.empty();
        M3Class counterpart = view.orElseThrow();
        int row = counterpart.row();
        return releases[row] == null ? Optional.empty()
                : Optional.of(new DeclaredAttachment(this, counterpart, releases[row], keys[row], epoch));
    }

    public synchronized boolean isCurrent(DeclaredAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        if (closed || !valid || attachment.session() != this
                || attachment.epoch() != epoch || attachment.type().owner() != index) {
            return false;
        }
        int row = attachment.type().row();
        return releases[row] == attachment.release() && keys[row] == attachment.candidateKey()
                && index.boundary().classAt(row).isPresent();
    }

    /** Conservative whole-snapshot retirement; preparation of a replacement is explicit. */
    public synchronized void invalidate() {
        requireOpen();
        if (valid) {
            valid = false;
            epoch++;
            Arrays.fill(releases, null);
            Arrays.fill(keys, null);
        }
    }

    public synchronized Statistics statistics() {
        return new Statistics(reused, observed, fallback, mismatches);
    }

    @Override public synchronized void close() {
        if (!closed) { invalidate(); closed = true; }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("session closed");
    }
}
