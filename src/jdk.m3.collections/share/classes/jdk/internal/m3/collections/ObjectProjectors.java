// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.function.BiFunction;
import jdk.internal.m3.collections.PoolProgressMonitor;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Safe explicit projection factories. No factory implicitly calls Object.toString(). */
//FLLDCIM: RELOCATE - donor blob 59388d348356 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public final class ObjectProjectors {
    private ObjectProjectors() { }

    public static <T> ObjectProjector<T> byString(
            int domain, Function<? super T, ? extends CharSequence> projection) {
        requireDomain(domain);
        Objects.requireNonNull(projection, "projection");
        return (value, monitor) -> {
            Objects.requireNonNull(monitor, "monitor");
            CharSequence text = Objects.requireNonNull(projection.apply(value), "string projection");
            return new ObjectSignal(
                ObjectProjectionKind.STRING,
                domain,
                text.length(),
                ObjectSignalMath.hashString(text),
                ObjectSignalMath.stringFeatures(text),
                0L,
                ObjectSignal.HAS_LENGTH | ObjectSignal.HAS_HASH);
        };
    }

    public static <T> ObjectProjector<T> byString(
            Function<? super T, ? extends CharSequence> projection) {
        return byString(ObjectSignalDomains.STRING, projection);
    }

    public static <T> ObjectProjector<T> byIndex(
            int domain, ToLongFunction<? super T> projection) {
        requireDomain(domain);
        Objects.requireNonNull(projection, "projection");
        return (value, monitor) -> {
            Objects.requireNonNull(monitor, "monitor");
            long raw = projection.applyAsLong(value);
            return new ObjectSignal(
                ObjectProjectionKind.INDEX,
                domain,
                1,
                ObjectSignalMath.mix64(raw),
                ObjectSignalMath.indexFeatures(raw),
                raw,
                ObjectSignal.HAS_LENGTH | ObjectSignal.HAS_HASH | ObjectSignal.HAS_IDENTITY);
        };
    }

    /** Caller supplies an already-derived bit signal. Exact verification remains external. */
    public static <T> ObjectProjector<T> bySignal(
            int domain, ToLongFunction<? super T> signalProjection) {
        requireDomain(domain);
        Objects.requireNonNull(signalProjection, "signalProjection");
        return (value, monitor) -> {
            Objects.requireNonNull(monitor, "monitor");
            return new ObjectSignal(
                ObjectProjectionKind.SIGNAL,
                domain,
                -1,
                0L,
                signalProjection.applyAsLong(value),
                0L,
                0);
        };
    }

    /**
     * Use a canonical encoder (value, monitor) -> bytes as the object->bytes boundary; an immutable codec's encode fits.
     * The encoded bytes are not retained by the signal table; only numeric summaries survive.
     */
    public static <T> ObjectProjector<T> byCanonicalBytes(
            int domain, BiFunction<? super T, ? super PoolProgressMonitor, byte[]> codec) {
        requireDomain(domain);
        Objects.requireNonNull(codec, "codec");
        return (value, monitor) -> {
            Objects.requireNonNull(monitor, "monitor");
            byte[] bytes = Objects.requireNonNull(codec.apply(value, monitor), "codec encoding");
            return new ObjectSignal(
                ObjectProjectionKind.CANONICAL_BYTES,
                domain,
                bytes.length,
                ObjectSignalMath.hashBytes(bytes),
                ObjectSignalMath.byteFeatures(bytes),
                0L,
                ObjectSignal.HAS_LENGTH | ObjectSignal.HAS_HASH);
        };
    }

    /** Generic escape hatch for domains with a richer hand-written signal contract. */
    public static <T> ObjectProjector<T> custom(Function<? super T, ObjectSignal> projection) {
        Objects.requireNonNull(projection, "projection");
        return (value, monitor) -> {
            Objects.requireNonNull(monitor, "monitor");
            return Objects.requireNonNull(projection.apply(value), "custom signal");
        };
    }

    private static void requireDomain(int domain) {
        if (domain < 0) throw new IllegalArgumentException("domain must be non-negative");
    }
}
