package jdk.internal.m3.common.progress;

/** Differential-build stub (tool only): the slice of the donor monitor the top-K kernels call. */
public interface ProgressMonitor {
    default void beginTask(String name, long totalWork) { }
    default void subTask(String name) { }
    default void worked(long work) { }
    default void done() { }
    default boolean isCanceled() { return false; }
    default void checkCanceled() {
        if (isCanceled()) throw new java.util.concurrent.CancellationException("Task canceled");
    }
}
