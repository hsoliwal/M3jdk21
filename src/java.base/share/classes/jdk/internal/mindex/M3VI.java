// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.module.ModuleDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * VI (Version Index): a caller-owned, bounded sorted version index. Precedence equality preserves distinct raw labels.
 * A scheme explicitly owns grammar and comparison; no scheme is silently treated as SemVer.
 * Built-in JPMS delegates to Java 21. Other grammars require their own explicit scheme.
 * Schemes must be deterministic, immutable and bound to a precise semantic revision.
 * This internal kernel stores references to existing JDK Strings, not a spelling dictionary.
 * It performs no module resolution, class loading, dependency selection or implementation routing.
 * Adapted from Synexia MIndexVersionTable; see m3/tooling/vi/README.md for pinned lineage.
 */
public final class M3VI<T> {
    /** JDK-owned callback boundary; null means no reporting or cancellation. */
    public interface Progress {
        default void begin(String task, long total) { }
        default void checkCancelled() { }
        default void worked(long work) { }
        default void done() { }
        static Progress none() { return NONE; }
    }

    private static final Progress NONE = new Progress() { };

    public interface Scheme<T> {
        String revision();
        T parse(String spelling);
        int compare(T left, T right);
    }

    private final Scheme<T> scheme;
    private final String revision;
    private final String[] labels;
    private final List<T> parsed;
    private final int[] order;

    private M3VI(Scheme<T> scheme, String revision,
            String[] labels, List<T> parsed, int[] order) {
        this.scheme = scheme; this.revision = revision;
        this.labels = labels; this.parsed = parsed; this.order = order;
    }

    public static <T> M3VI<T> prepare(
            List<String> versions, Scheme<T> scheme, int maxVersions,
            Progress monitor) {
        Objects.requireNonNull(versions, "versions"); Objects.requireNonNull(scheme, "scheme");
        if (maxVersions < 1 || maxVersions > 1_000_000 || versions.size() > maxVersions) {
            throw new IllegalArgumentException("version budget");
        }
        Progress progress = monitor == null ? Progress.none() : monitor;
        String revision = Objects.requireNonNull(scheme.revision(), "scheme revision");
        if (revision.length() == 0) throw new IllegalArgumentException("empty scheme revision");
        String[] labels = versions.toArray(String[]::new);
        java.util.ArrayList<T> parsed = new java.util.ArrayList<>(labels.length);
        Integer[] transientOrder = new Integer[labels.length];
        progress.begin("M3VI.prepare", labels.length);
        try {
            for (int i = 0; i < labels.length; i++) {
                progress.checkCancelled();
                Objects.requireNonNull(labels[i], "version");
                if (labels[i].length() > 16_384) throw new IllegalArgumentException("version length budget");
                parsed.add(Objects.requireNonNull(scheme.parse(labels[i]), "parsed version"));
                transientOrder[i] = i;
                progress.worked(1);
            }
            Arrays.sort(transientOrder, (a, b) -> {
                progress.checkCancelled();
                int compared = scheme.compare(parsed.get(a), parsed.get(b));
                return compared == 0 ? Integer.compare(a, b) : compared;
            });
            progress.checkCancelled();
            int[] order = new int[labels.length];
            for (int i = 0; i < order.length; i++) order[i] = transientOrder[i];
            if (!revision.equals(scheme.revision())) {
                throw new IllegalStateException("scheme revision changed during preparation");
            }
            return new M3VI<>(scheme, revision, labels, List.copyOf(parsed), order);
        } finally { progress.done(); }
    }

    public static Scheme<ModuleDescriptor.Version> jpms21() {
        return new Scheme<>() {
            @Override public String revision() { return "jpms-version-java21"; }
            @Override public ModuleDescriptor.Version parse(String value) {
                return ModuleDescriptor.Version.parse(value);
            }
            @Override public int compare(ModuleDescriptor.Version a, ModuleDescriptor.Version b) {
                return a.compareTo(b);
            }
        };
    }

    public int size() { return order.length; }
    public String schemeRevision() { return revision; }
    public int originalRow(int sortedPosition) { return order[Objects.checkIndex(sortedPosition, size())]; }
    public String label(int originalRow) { return labels[Objects.checkIndex(originalRow, size())]; }

    /** Returns original rows in stable precedence order; output work is proportional to result size. */
    public int[] between(String lower, boolean includeLower,
            String upper, boolean includeUpper) {
        requireRevision();
        T lo = parseBound(lower); T hi = parseBound(upper);
        if (scheme.compare(lo, hi) > 0) throw new IllegalArgumentException("reversed bounds");
        int from = bound(lo, !includeLower);
        int to = bound(hi, includeUpper);
        return to <= from ? new int[0] : Arrays.copyOfRange(order, from, to);
    }

    private T parseBound(String value) {
        Objects.requireNonNull(value, "bound");
        if (value.length() > 16_384) throw new IllegalArgumentException("version length budget");
        return Objects.requireNonNull(scheme.parse(value), "parsed bound");
    }

    private void requireRevision() {
        if (!revision.equals(scheme.revision())) throw new IllegalStateException("scheme revision changed");
    }

    private int bound(T value, boolean afterEquals) {
        int low = 0, high = order.length;
        while (low < high) {
            int mid = low + (high - low) / 2;
            int compared = scheme.compare(parsed.get(order[mid]), value);
            if (compared < 0 || afterEquals && compared == 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }
}
