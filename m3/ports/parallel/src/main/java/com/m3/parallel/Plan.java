/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable launch plan: backend, kernel, grid and bound buffers, plus the {@link Session} that owns the plan's
 * device storage.
 *
 * <p>Built once with {@link #builder(Backend)}. Each {@link #execute()} applies the {@link Transfer} policy of every
 * binding, launches the kernel over the grid and returns a {@link Result} for explicit downloads and uploads.
 * {@link #close()} releases device storage. The record is shallowly immutable; all mutable state is device state held
 * by {@code session}, which is safe for use from several threads (calls are serialized).</p>
 *
 * @param backend  executing backend
 * @param kernel   kernel id
 * @param grid     launch grid
 * @param bindings bound host buffers, in kernel argument order
 * @param session  device state prepared for exactly these components
 */
public record Plan(Backend backend, KernelId kernel, Grid1D grid, List<Binding> bindings, Session session)
        implements AutoCloseable {

    /**
     * Validates that {@code session} was prepared for these components.
     *
     * @param backend  executing backend
     * @param kernel   kernel id
     * @param grid     launch grid
     * @param bindings bound host buffers
     * @param session  device state
     */
    public Plan {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(grid, "grid");
        bindings = List.copyOf(bindings);
        Objects.requireNonNull(session, "session");
        if (!session.preparedFor(backend, kernel, grid, bindings)) {
            throw new IllegalArgumentException("session was prepared for a different plan");
        }
    }

    /**
     * Starts building a plan for {@code backend}.
     *
     * @param backend executing backend
     * @return a new builder
     */
    public static Builder builder(Backend backend) {
        return new Builder(backend);
    }

    /**
     * Applies the transfer policies, launches the kernel once over the grid and waits for completion.
     *
     * @return the result of this launch
     * @throws IllegalStateException if the plan is closed
     */
    public Result execute() {
        return session.execute(this);
    }

    /**
     * Reports whether device storage is still held.
     *
     * @return {@code true} until {@link #close()}
     */
    public boolean isOpen() {
        return session.isOpen();
    }

    /** Releases device storage. Idempotent. */
    @Override
    public void close() {
        session.close();
    }

    /**
     * One bound buffer and its host-to-device transfer policy.
     *
     * @param buffer   host buffer
     * @param transfer transfer policy
     */
    public record Binding(IntBuffer buffer, Transfer transfer) {
        /**
         * Validates the binding.
         *
         * @param buffer   host buffer
         * @param transfer transfer policy
         */
        public Binding {
            Objects.requireNonNull(buffer, "buffer");
            Objects.requireNonNull(transfer, "transfer");
        }
    }

    /** Mutable, single-use builder of a {@link Plan}. */
    public static final class Builder {
        private final Backend backend;
        private final List<Binding> bindings = new ArrayList<>();
        private KernelId kernel;
        private Grid1D grid;

        private Builder(Backend backend) {
            this.backend = Objects.requireNonNull(backend, "backend");
        }

        /**
         * Sets the kernel.
         *
         * @param kernel kernel id
         * @return this builder
         */
        public Builder kernel(KernelId kernel) {
            this.kernel = Objects.requireNonNull(kernel, "kernel");
            return this;
        }

        /**
         * Sets the launch grid.
         *
         * @param grid launch grid
         * @return this builder
         */
        public Builder grid(Grid1D grid) {
            this.grid = Objects.requireNonNull(grid, "grid");
            return this;
        }

        /**
         * Binds the next kernel argument.
         *
         * @param buffer   host buffer; each buffer may be bound once
         * @param transfer transfer policy
         * @return this builder
         */
        public Builder bind(IntBuffer buffer, Transfer transfer) {
            Binding binding = new Binding(buffer, transfer);
            if (bindings.stream().anyMatch(existing -> existing.buffer() == buffer)) {
                throw new IllegalArgumentException("buffer is already bound: " + buffer);
            }
            bindings.add(binding);
            return this;
        }

        /**
         * Prepares device storage and returns the immutable plan.
         *
         * @return the plan
         * @throws IllegalStateException    if the kernel, grid or bindings are missing
         * @throws IllegalArgumentException if the backend cannot execute the kernel
         */
        public Plan build() {
            if (kernel == null || grid == null || bindings.isEmpty()) {
                throw new IllegalStateException("kernel, grid and at least one binding are required");
            }
            if (!backend.supports(kernel)) {
                throw new IllegalArgumentException("backend " + backend.id() + " cannot execute kernel " + kernel);
            }
            List<Binding> frozen = List.copyOf(bindings);
            return new Plan(backend, kernel, grid, frozen, new Session(backend, kernel, grid, frozen));
        }
    }
}
