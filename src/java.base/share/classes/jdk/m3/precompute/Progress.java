// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/** Minimal target-owned progress/cancellation contract for bounded precompute work. */
public interface Progress {
    Progress NONE = new Progress() {};

    default void begin(String task, long totalWork) {}
    default void subTask(String task) {}
    default void worked(long work) {}
    default void checkCanceled() {}
    default void done() {}

    static Progress none() {
        return NONE;
    }
}
