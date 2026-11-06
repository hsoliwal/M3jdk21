package jdk.internal.m3.job;

/** Differential-build stub (tool only): the slice of jdk.internal.m3.job.IProgressMonitor the search kernels call. */
public interface IProgressMonitor {
    boolean isCanceled();

    default void checkCanceled() {
        if (isCanceled()) throw new java.util.concurrent.CancellationException("Task canceled");
    }

    static IProgressMonitor noop() {
        return () -> false;
    }
}
