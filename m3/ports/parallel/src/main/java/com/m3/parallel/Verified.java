/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Content-addressed receipt of a plan run that matched the pure-Java CPU reference.
 *
 * <p>{@link #run(Plan, int)} snapshots every bound host buffer, runs the plan on its backend and an independent copy
 * on {@link Backend#reference()}, downloads every binding from both, and admits the run only if every buffer is equal
 * element for element. The receipt root is a length-framed SHA-256 over the backend id, kernel, grid, launch count and
 * the input and output binding roots; the compact constructor recomputes it and rejects a mismatch.</p>
 *
 * @param backend    id of the verified backend
 * @param kernel     kernel id
 * @param grid       launch grid
 * @param launches   number of launches run on both sides
 * @param inputRoot  SHA-256 of the bindings before the first launch
 * @param outputRoot SHA-256 of the bindings after the last launch
 * @param root       receipt root; {@code null} to compute
 */
public record Verified(String backend, KernelId kernel, Grid1D grid, int launches, String inputRoot,
        String outputRoot, String root) {
    private static final String RECEIPT_DOMAIN = "M3_PARALLEL_VERIFIED_V1";

    /**
     * Validates the fields and recomputes the root.
     *
     * @param backend    id of the verified backend
     * @param kernel     kernel id
     * @param grid       launch grid
     * @param launches   number of launches, {@code >= 1}
     * @param inputRoot  SHA-256 hex of the inputs
     * @param outputRoot SHA-256 hex of the outputs
     * @param root       receipt root, or {@code null} to compute
     */
    public Verified {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(grid, "grid");
        if (launches < 1) {
            throw new IllegalArgumentException("launches must be >= 1");
        }
        inputRoot = Digests.requireSha256(inputRoot, "inputRoot");
        outputRoot = Digests.requireSha256(outputRoot, "outputRoot");
        String expected = Digests.sha256(RECEIPT_DOMAIN, backend, kernel.name(), Integer.toString(grid.global()),
                Integer.toString(grid.local()), Integer.toString(launches), inputRoot, outputRoot);
        if (root == null) {
            root = expected;
        } else if (!root.equals(expected)) {
            throw new IllegalArgumentException("receipt root mismatch");
        }
    }

    /**
     * Runs a fresh plan once and verifies it against the CPU reference.
     *
     * @param plan an open plan that has not been executed
     * @return the receipt
     */
    public static Verified run(Plan plan) {
        return run(plan, 1);
    }

    /**
     * Runs a fresh plan {@code launches} times and verifies every bound buffer against the CPU reference. On return
     * the plan's host buffers hold the verified device contents and the plan stays open.
     *
     * @param plan     an open plan that has not been executed
     * @param launches number of launches, {@code >= 1}
     * @return the receipt
     * @throws IllegalStateException if the plan is closed or already executed, or if any buffer differs
     */
    public static Verified run(Plan plan, int launches) {
        Objects.requireNonNull(plan, "plan");
        if (launches < 1) {
            throw new IllegalArgumentException("launches must be >= 1");
        }
        if (!plan.isOpen() || plan.session().launches() != 0) {
            throw new IllegalStateException("verification needs an open plan that has not been executed");
        }
        String inputRoot = Digests.bindingsRoot(plan.bindings());
        List<IntBuffer> expected = plan.bindings().stream().map(binding -> binding.buffer().copy()).toList();
        try (Plan reference = referencePlan(plan, expected)) {
            launch(reference, launches).downloadAll();
        }
        launch(plan, launches).downloadAll();
        IntStream.range(0, expected.size()).forEach(slot -> compare(plan, slot, expected.get(slot)));
        return new Verified(plan.backend().id(), plan.kernel(), plan.grid(), launches, inputRoot,
                Digests.bindingsRoot(plan.bindings()), null);
    }

    private static Plan referencePlan(Plan plan, List<IntBuffer> buffers) {
        Plan.Builder builder = Plan.builder(Backend.reference()).kernel(plan.kernel()).grid(plan.grid());
        IntStream.range(0, buffers.size())
                .forEach(slot -> builder.bind(buffers.get(slot), plan.bindings().get(slot).transfer()));
        return builder.build();
    }

    private static Result launch(Plan plan, int launches) {
        return IntStream.range(0, launches).mapToObj(ignored -> plan.execute()).reduce((first, last) -> last)
                .orElseThrow();
    }

    private static void compare(Plan plan, int slot, IntBuffer expected) {
        int index = expected.mismatch(plan.bindings().get(slot).buffer());
        if (index >= 0) {
            throw new IllegalStateException("backend " + plan.backend().id() + " diverged from the CPU reference: "
                    + plan.kernel() + " binding " + slot + " index " + index);
        }
    }
}
