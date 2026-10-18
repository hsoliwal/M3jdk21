#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Author and seal comment-only progress documentation; receiving is SDK-only."""
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import sys

sys.dont_write_bytecode = True
WORK = Path(__file__).resolve().parents[2]
HERE = WORK / "runtime-refinement"
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
BASELINE = WORK / "M3jdk21"
OWNER = HERE / "canonical/synexia-openrewrite-recipes"
CRATE = OWNER / "recipe-crates/m3-collections-contract-refinement-20261017"
CANDIDATE = HERE / "candidates/progress-doclint"
PREFIX = "src/java.base/share/classes/"
EDITS = {}


def replace(file, before, after):
    EDITS.setdefault(PREFIX + file, []).append((before, after))


replace("java/lang/Runnable.java", "     * A pool must bind it explicitly when dispatching work asynchronously.\n", """     * A pool must bind it explicitly when dispatching work asynchronously.
     *
     * @return the current progress monitor, never {@code null}
""")
replace("java/lang/Runnable.java", "     * Existing run() remains the sole abstract method.\n", """     * Existing run() remains the sole abstract method.
     *
     * @param monitor the borrowed monitor to bind during this operation
     * @throws NullPointerException if {@code monitor} is {@code null}
     * @throws java.util.concurrent.CancellationException if cancellation is
     *         observed before or after this operation
""")
replace("java/util/concurrent/Callable.java", "    /** A borrowed monitor is scoped for this call; call() remains abstract. */", """    /**
     * Computes this task's result with a borrowed monitor scoped to this call.
     * The no-argument {@link #call()} remains the sole abstract method.
     *
     * @param monitor the borrowed monitor to bind during this call
     * @return the computed result
     * @throws NullPointerException if {@code monitor} is {@code null}
     * @throws CancellationException if cancellation is observed before or
     *         after this call
     * @throws Exception if this task cannot compute a result
     */""")
replace("java/util/concurrent/Executor.java", "     * before switching threads. Existing execute(Runnable) is unchanged.\n", """     * before switching threads. Existing execute(Runnable) is unchanged.
     * The monitor is borrowed and is not completed by the dispatched task.
     *
     * @param command the runnable task
     * @param monitor the monitor to bind while the task runs
     * @throws NullPointerException if {@code command} or {@code monitor} is null
     * @throws RejectedExecutionException if this task cannot be accepted
     */PLACEHOLDER
""".replace("     */PLACEHOLDER\n", ""))
replace("java/util/concurrent/Executor.java", "    /** Capture the current lexical or process-wide monitor at submission. */", """    /**
     * Captures the current lexical or process-wide monitor at submission.
     *
     * @param command the runnable task
     * @throws NullPointerException if {@code command} is {@code null}
     * @throws RejectedExecutionException if this task cannot be accepted
     */""")
replace("java/util/concurrent/ExecutorService.java", "     * Future cancellation and cooperative monitor cancellation remain distinct.\n", """     * Future cancellation and cooperative monitor cancellation remain distinct.
     * The monitor is bound during execution and is not completed by this task.
     *
     * @param <T> the task's result type
     * @param task the task to submit
     * @param monitor the borrowed monitor to bind during the task
     * @return a future representing pending completion of the task
     * @throws NullPointerException if {@code task} or {@code monitor} is null
     * @throws RejectedExecutionException if the task cannot be scheduled
""")
replace("java/util/concurrent/ExecutorService.java", "    /** Avoids overloading submit(Runnable,T), which would change overload resolution. */", """    /**
     * Submits a runnable with a borrowed monitor scoped to its execution.
     * Avoids overloading {@code submit(Runnable,T)}, which would change
     * overload resolution. Completion does not finish the borrowed monitor.
     *
     * @param task the task to submit
     * @param monitor the borrowed monitor to bind during the task
     * @return a future whose successful completion result is {@code null}
     * @throws NullPointerException if {@code task} or {@code monitor} is null
     * @throws RejectedExecutionException if the task cannot be scheduled
     */""")
replace("java/util/concurrent/MonitoredCallable.java", "    /** Replaceable checked body, with no ownership over monitor completion. */", """    /**
     * Computes a result using the supplied borrowed progress monitor.
     * This body does not own completion of the monitor.
     *
     * @param monitor the monitor bound by the template call
     * @return the computed result
     * @throws Exception if the body cannot compute a result
     */""")
replace("java/util/concurrent/MonitoredCallable.java", "    /** Template method: inherit monitor, check cancellation, dispatch body. */", """    /**
     * Inherits the current monitor, checks cancellation, and dispatches the body.
     * Cancellation is checked again after the body returns.
     *
     * @return the result computed by {@link #callMonitored(ProgressMonitor)}
     * @throws CancellationException if cancellation is observed before or
     *         after the body
     * @throws Exception if the body cannot compute a result
     */""")
replace("java/util/concurrent/MonitoredRunnable.java", "    /** Replaceable task body; implementations report progress through monitor. */", """    /**
     * Runs the task body using the supplied borrowed progress monitor.
     *
     * @param monitor the monitor bound by the template run
     */""")
replace("java/util/concurrent/MonitoredRunnable.java", "    /** Template method: inherit monitor, check cancellation, dispatch body. */", """    /**
     * Inherits the current monitor, checks cancellation, and dispatches the body.
     * Cancellation is checked again after the body returns.
     *
     * @throws CancellationException if cancellation is observed before or
     *         after the body
     */""")
replace("java/util/concurrent/ProgressMonitor.java", "    long UNKNOWN_WORK = -1L;", """    /** Indicates that the total amount of work is not known. */
    long UNKNOWN_WORK = -1L;""")
replace("java/util/concurrent/ProgressMonitor.java", "    ProgressMonitor NONE = new ProgressMonitor() { };", """    /**
     * A monitor with no progress state and no local cancellation state.
     * Its inherited {@link #checkCancelled()} still observes interruption
     * of the calling thread.
     */
    ProgressMonitor NONE = new ProgressMonitor() { };""")
replace("java/util/concurrent/ProgressMonitor.java", "    default void begin(String task, long totalWork) { }", """    /**
     * Announces a task and its estimated work. The default implementation
     * does nothing; implementations define their task lifecycle and validation.
     *
     * @param task the task description
     * @param totalWork the estimated work, or {@link #UNKNOWN_WORK}
     */
    default void begin(String task, long totalWork) { }""")
replace("java/util/concurrent/ProgressMonitor.java", "    default void worked(long units) { }", """    /**
     * Reports additional completed work. The default implementation does nothing.
     *
     * @param units the additional work units, with validation defined by the
     *              implementation
     */
    default void worked(long units) { }""")
replace("java/util/concurrent/ProgressMonitor.java", "    default void message(String text) { }", """    /**
     * Reports a progress message. The default implementation does nothing.
     *
     * @param text the progress message
     */
    default void message(String text) { }""")
replace("java/util/concurrent/ProgressMonitor.java", "    default boolean isCancelled() { return false; }", """    /**
     * Reports cooperative cancellation state. The default returns {@code false}.
     *
     * @return whether this monitor reports cancellation
     */
    default boolean isCancelled() { return false; }""")
replace("java/util/concurrent/ProgressMonitor.java", "    default void cancel() { }", """    /** Requests cooperative cancellation. The default implementation does nothing. */
    default void cancel() { }""")
replace("java/util/concurrent/ProgressMonitor.java", "    default void done() { }", """    /**
     * Completes work owned by this monitor. The default implementation does
     * nothing. A task must not complete a monitor borrowed from its caller.
     */
    default void done() { }""")
replace("java/util/concurrent/ProgressMonitor.java", "    default void checkCancelled() {", """    /**
     * Checks this monitor and the calling thread for cancellation without
     * clearing the thread's interrupt status.
     *
     * @throws CancellationException if {@link #isCancelled()} is true or
     *         the calling thread is interrupted
     */
    default void checkCancelled() {""")
replace("java/util/concurrent/ProgressMonitor.java", "    /** Allocate parent ticks to a child; complete the child independently. */", """    /**
     * Allocates parent ticks to a child that can be completed independently.
     * Delegates to {@link ProgressMonitors#child(ProgressMonitor, String, long, long)}.
     *
     * @param name the child description, or {@code null} for no description
     * @param parentTicks the nonnegative work budget credited to this monitor
     * @param totalWork the positive child work estimate, or {@link #UNKNOWN_WORK}
     * @return the child monitor, or {@link #NONE} when this monitor is {@code NONE}
     * @throws IllegalArgumentException if the work budget or estimate is invalid
     */""")
replace("java/util/concurrent/ProgressMonitors.java", "    /** Process fallback. Supplier publication is atomic; it may be called concurrently. */", """    /**
     * Gets the process fallback monitor. Supplier publication is atomic; the
     * supplier may be called concurrently and must support that use.
     *
     * @return the provider's monitor, or {@link ProgressMonitor#NONE} if it
     *         supplies {@code null}
     */""")
replace("java/util/concurrent/ProgressMonitors.java", "    /** Update the global fallback and return the previous provider. */", """    /**
     * Atomically replaces the global fallback provider.
     *
     * @param provider the provider to use when no lexical monitor is bound
     * @return the previous provider
     * @throws NullPointerException if {@code provider} is {@code null}
     */""")
replace("java/util/concurrent/ProgressMonitors.java", "    public static void resetSystem() { SYSTEM.set(NONE); }", """    /** Restores the global provider that supplies {@link ProgressMonitor#NONE}. */
    public static void resetSystem() { SYSTEM.set(NONE); }""")
replace("java/util/concurrent/ProgressMonitors.java", "    /** The lexical monitor, or the process fallback when no scope is active. */", """    /**
     * Gets the lexical monitor, or the process fallback when no scope is active.
     *
     * @return the current monitor, never {@code null}
     */""")
replace("java/util/concurrent/ProgressMonitors.java", "    /** Bind an already-owned monitor: closing this scope does not call done(). */", """    /**
     * Binds a borrowed monitor to this thread until the returned scope closes.
     * Scopes must close on their creating thread in reverse creation order.
     * Closing a scope does not complete its borrowed monitor. Binding is local
     * to this thread and is not automatically propagated to asynchronous tasks.
     *
     * @param monitor the borrowed monitor
     * @return the scope that restores the previous binding when closed
     * @throws NullPointerException if {@code monitor} is {@code null}
     */""")
replace("java/util/concurrent/ProgressMonitors.java", "    public static void run(Runnable task, ProgressMonitor monitor) {", """    /**
     * Runs a task with a borrowed monitor, restoring the previous binding on exit.
     * Cancellation is checked before and after the task, and the monitor is
     * not completed by this method.
     *
     * @param task the task to run
     * @param monitor the borrowed monitor
     * @throws NullPointerException if {@code task} or {@code monitor} is null
     * @throws CancellationException if cancellation is observed before or
     *         after the task
     */
    public static void run(Runnable task, ProgressMonitor monitor) {""")
replace("java/util/concurrent/ProgressMonitors.java", "    public static <T> T call(Callable<T> task, ProgressMonitor monitor) throws Exception {", """    /**
     * Calls a task with a borrowed monitor, restoring the previous binding on exit.
     * Cancellation is checked before and after the task, and the monitor is
     * not completed by this method.
     *
     * @param <T> the result type
     * @param task the task to call
     * @param monitor the borrowed monitor
     * @return the computed result
     * @throws NullPointerException if {@code task} or {@code monitor} is null
     * @throws CancellationException if cancellation is observed before or
     *         after the task
     * @throws Exception if the task cannot compute a result
     */
    public static <T> T call(Callable<T> task, ProgressMonitor monitor) throws Exception {""")
replace("java/util/concurrent/ProgressMonitors.java", "    public static ProgressMonitor child(\n", """    /**
     * Creates a child whose completed work is scaled to a bounded parent budget.
     * Completing the child credits the remaining budget without completing its
     * parent. A child observes parent cancellation, while cancelling the child
     * does not cancel its parent. A zero budget still observes cancellation.
     * If the parent is {@link ProgressMonitor#NONE}, returns that monitor.
     *
     * <p>The child uses atomic local accounting. Its parent must support any
     * concurrent callbacks caused by callers sharing the child.
     *
     * @param parent the borrowed parent monitor
     * @param name the child description, or {@code null} for no description
     * @param parentTicks the nonnegative work budget credited to the parent
     * @param totalWork the positive child work estimate, or
     *                  {@link ProgressMonitor#UNKNOWN_WORK} to credit only at completion
     * @return the child monitor, or {@code NONE} when the parent is {@code NONE}
     * @throws NullPointerException if {@code parent} is {@code null}
     * @throws IllegalArgumentException if {@code parentTicks} is negative or
     *         {@code totalWork} is neither positive nor {@code UNKNOWN_WORK}
     */
    public static ProgressMonitor child(
""")
replace("java/util/concurrent/ProgressMonitors.java", "        @Override public void close() {", """        /**
         * Restores this thread's previous progress binding. Repeated close on
         * the owning thread has no effect after a successful close.
         *
         * @throws IllegalStateException if called on another thread, or if an
         *         open nested scope must be closed first
         */
        @Override public void close() {""")


def digest(data):
    return hashlib.sha256(data).hexdigest()


def strip_javadoc(source):
    # Only Javadoc changes are permitted. No line comments, strings, ordinary
    # block comments, whitespace outside Javadoc, declarations or bodies change.
    return re.sub(r"/\*\*.*?\*/", "", source, flags=re.S)


def main():
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=BASELINE, text=True).strip()
    if head != BASE:
        raise ValueError("baseline commit drift: " + head)
    rows = []
    for relative, edits in sorted(EDITS.items()):
        before = (BASELINE / relative).read_text()
        after = before
        for needle, replacement in edits:
            if after.count(needle) != 1:
                raise ValueError("documentation anchor drift: " + relative + " " + needle)
            after = after.replace(needle, replacement)
        # Added Javadocs introduce indentation/newlines. All noncomment tokens
        # remain exact; the separate -g:none class comparison checks compiler output.
        if re.sub(r"\s+", "", strip_javadoc(before)) != re.sub(r"\s+", "", strip_javadoc(after)):
            raise ValueError("non-documentation mutation: " + relative)
        target = CANDIDATE / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(after)
        rows.append((relative, digest(before.encode()), digest(after.encode()), Path(relative).name + ".txt"))
    manifest = CANDIDATE / "m3/compatibility/m3-progress-doclint-20261018.tsv"
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text("# Exact documentation successor after M3jdk21 #1082 and #1091.\n"
                        "# path\\tbefore_sha256\\tafter_sha256\\ttemplate\n" +
                        "".join("\t".join(row) + "\n" for row in rows))
    spec = importlib.util.spec_from_file_location("doclint_prepare", CRATE / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    metadata = prepare.seal(CRATE, "m3jdk21-progress-doclint", "hsoliwal/M3jdk21", BASELINE,
                            [CANDIDATE], BASE, OWNER)
    receipt = {"schema": "M3_PROGRESS_DOCLINT_CANDIDATE_V1", "baseline": BASE,
               "manifest_sha256": digest(manifest.read_bytes()), "sources": rows,
               "receiving_source_written": False, "metadata": metadata,
               "scope": "Javadoc only; bytecode equivalence and doclint must be freshly executed"}
    (HERE / "authoring/progress-doclint-candidate-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
