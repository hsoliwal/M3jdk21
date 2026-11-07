package com.synexia.job;
/** Donor-side stub (differential only): the calls the donor trie core makes; never canceled. */
public interface IProgressMonitor {
    default void beginTask(String name, long work) { }
    default void worked(long work) { }
    default void done() { }
    default void subTask(String name) { }
    default boolean isCanceled() { return false; }
    default void checkCanceled() { if (isCanceled()) throw new java.util.concurrent.CancellationException(); }
    static IProgressMonitor noop() { return new IProgressMonitor() { }; }
}
