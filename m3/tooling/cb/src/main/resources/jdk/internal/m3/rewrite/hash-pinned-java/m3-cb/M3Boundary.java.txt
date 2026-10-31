// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional weak JVM boundary for a frozen M3CI.
 *
 * <p>The primitive index never strongly owns Class or ClassLoader objects. Standard snapshots keep
 * weak round-trip handles; compact snapshots retain only class/loader counts and allocate no
 * WeakReference objects at all.</p>
 */
public final class M3Boundary {
    private static final WeakReference<?>[] EMPTY_REFERENCES = new WeakReference<?>[0];

    private final WeakReference<?>[] classes;
    private final WeakReference<?>[] loaders;
    private final int classCount;
    private final int loaderSpaceCount;

    private M3Boundary(
            WeakReference<?>[] classes,
            WeakReference<?>[] loaders,
            int classCount,
            int loaderSpaceCount) {
        this.classes = classes;
        this.loaders = loaders;
        this.classCount = classCount;
        this.loaderSpaceCount = loaderSpaceCount;
    }

    static M3Boundary of(
            Class<?>[] classes, ClassLoader[] loaders, boolean retainWeakBoundary) {
        Objects.requireNonNull(classes, "classes");
        Objects.requireNonNull(loaders, "loaders");
        if (!retainWeakBoundary) {
            return new M3Boundary(
                    EMPTY_REFERENCES,
                    EMPTY_REFERENCES,
                    classes.length,
                    loaders.length);
        }

        WeakReference<?>[] classRefs = new WeakReference<?>[classes.length];
        for (int i = 0; i < classes.length; i++) {
            classRefs[i] =
                    new WeakReference<>(
                            Objects.requireNonNull(classes[i], "classes[" + i + "]"));
        }
        WeakReference<?>[] loaderRefs = new WeakReference<?>[loaders.length];
        for (int i = 0; i < loaders.length; i++) {
            loaderRefs[i] = new WeakReference<>(loaders[i]);
        }
        return new M3Boundary(
                classRefs, loaderRefs, classes.length, loaders.length);
    }

    public boolean retained() {
        return classes.length != 0;
    }

    public int classCount() {
        return classCount;
    }

    public int loaderSpaceCount() {
        return loaderSpaceCount;
    }

    public Optional<Class<?>> classAt(int row) {
        Objects.checkIndex(row, classCount);
        if (!retained()) return Optional.empty();
        Object value = classes[row].get();
        return value instanceof Class<?> type ? Optional.of(type) : Optional.empty();
    }

    public Optional<ClassLoader> loaderAt(int loaderSpace) {
        Objects.checkIndex(loaderSpace, loaderSpaceCount);
        if (loaderSpace == 0 || !retained()) {
            return Optional.empty();
        }
        Object value = loaders[loaderSpace].get();
        return value instanceof ClassLoader loader ? Optional.of(loader) : Optional.empty();
    }

    /**
     * Snapshot-local loader coordinate. Bootstrap loading is always zero; -1 means unknown,
     * detached, or already collected.
     */
    public int loaderSpaceOf(ClassLoader loader) {
        if (loader == null) {
            return 0;
        }
        if (!retained()) return -1;
        for (int i = 1; i < loaders.length; i++) {
            if (loaders[i].get() == loader) {
                return i;
            }
        }
        return -1;
    }

    public int liveClassCount() {
        if (!retained()) return 0;
        int count = 0;
        for (WeakReference<?> reference : classes) {
            if (reference.get() != null) count++;
        }
        return count;
    }

    public int liveLoaderCount() {
        if (!retained()) return 0;
        int count = 1;
        for (int i = 1; i < loaders.length; i++) {
            if (loaders[i].get() != null) count++;
        }
        return count;
    }

    /** Approximate retained WeakReference object count; zero for compact snapshots. */
    public int retainedReferenceCount() {
        return retained() ? classes.length + loaders.length : 0;
    }
}
