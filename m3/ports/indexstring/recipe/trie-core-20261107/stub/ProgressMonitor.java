package com.synexia.common.progress;
/** Donor-side stub (differential only). */
public interface ProgressMonitor {
    default void checkCanceled() { }
    default void worked(long work) { }
}
