/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import java.util.Objects;

/**
 * Handle of one completed launch, used for explicit host/device transfers.
 *
 * <p>A result is current until the next {@link Plan#execute()} of the same plan; transfers through a superseded
 * result fail with {@link IllegalStateException}, so a caller never reads device state it did not ask for.</p>
 */
public final class Result {
    private final Plan plan;
    private final long launch;

    Result(Plan plan, long launch) {
        this.plan = plan;
        this.launch = launch;
    }

    /**
     * Returns the plan that produced this result.
     *
     * @return the plan
     */
    public Plan plan() {
        return plan;
    }

    /**
     * Returns the 1-based launch number of this result.
     *
     * @return launch number
     */
    public long launch() {
        return launch;
    }

    /**
     * Copies the device contents of the given bound buffers into those host buffers.
     *
     * @param buffers bound host buffers
     * @return this result
     * @throws IllegalArgumentException if a buffer is not bound to the plan
     * @throws IllegalStateException    if the plan is closed or this result is stale
     */
    public Result download(IntBuffer... buffers) {
        Objects.requireNonNull(buffers, "buffers");
        plan.session().download(launch, buffers.clone());
        return this;
    }

    /**
     * Copies the device contents of every bound buffer into its host buffer.
     *
     * @return this result
     */
    public Result downloadAll() {
        return download(plan.bindings().stream().map(Plan.Binding::buffer).toArray(IntBuffer[]::new));
    }

    /**
     * Copies a bound host buffer to the device, for example to patch resident state between launches.
     *
     * @param buffer bound host buffer
     * @return this result
     * @throws IllegalArgumentException if the buffer is not bound to the plan
     * @throws IllegalStateException    if the plan is closed or this result is stale
     */
    public Result upload(IntBuffer buffer) {
        plan.session().upload(launch, Objects.requireNonNull(buffer, "buffer"));
        return this;
    }

    @Override
    public String toString() {
        return "Result[" + plan.backend().id() + "/" + plan.kernel() + " launch=" + launch + "]";
    }
}
