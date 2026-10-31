/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel.testing;

import com.m3.parallel.Backend;
import com.m3.parallel.Grid1D;
import com.m3.parallel.IntBuffer;
import com.m3.parallel.JavaKernel;
import com.m3.parallel.JavaKernels;
import com.m3.parallel.KernelId;
import com.m3.parallel.spi.BackendProvider;
import com.m3.parallel.spi.DeviceProgram;
import com.m3.parallel.spi.Probe;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.IntStream;

/** Test-only backend providers registered in {@code META-INF/services}; each exercises one discovery rule. */
public final class TestProviders {
    private TestProviders() {
    }

    /** Base: sequential Java execution of registered kernels. */
    public abstract static class Sequential implements BackendProvider {
        /** Creates the provider. */
        protected Sequential() {
        }

        @Override
        public Set<Backend.Capability> capabilities() {
            return EnumSet.of(Backend.Capability.CPU);
        }

        @Override
        public int score() {
            return -10;
        }

        @Override
        public Probe probe() {
            return Probe.ready();
        }

        @Override
        public boolean supports(KernelId kernel) {
            return JavaKernels.lookup(kernel).isPresent();
        }

        @Override
        public DeviceProgram prepare(KernelId kernel, Grid1D grid, int[] bufferLengths) {
            return new SequentialProgram(JavaKernels.lookup(kernel).orElseThrow(), grid, bufferLengths, corrupt());
        }

        /**
         * Reports whether downloads flip the low bit of element 0.
         *
         * @return {@code true} for the faulty provider
         */
        protected boolean corrupt() {
            return false;
        }
    }

    /** Available, never preferred, and corrupts every download: proves {@code Verified} detects divergence. */
    public static final class Faulty extends Sequential {
        /** Creates the provider. */
        public Faulty() {
        }

        @Override
        public String id() {
            return "test-faulty";
        }

        @Override
        protected boolean corrupt() {
            return true;
        }
    }

    /** Unavailable: no driver. */
    public static final class Absent extends Sequential {
        /** Creates the provider. */
        public Absent() {
        }

        @Override
        public String id() {
            return "test-absent";
        }

        @Override
        public Probe probe() {
            return Probe.unavailable("no driver");
        }
    }

    /** Probe throws: must be recorded as unavailable, never crash discovery. */
    public static final class Throwing extends Sequential {
        /** Creates the provider. */
        public Throwing() {
        }

        @Override
        public String id() {
            return "test-throwing";
        }

        @Override
        public Probe probe() {
            throw new IllegalStateException("driver crashed");
        }
    }

    /** Invalid id. */
    public static final class BadId extends Sequential {
        /** Creates the provider. */
        public BadId() {
        }

        @Override
        public String id() {
            return "Bad Id";
        }
    }

    /** Claims the reserved reference id. */
    public static final class Reserved extends Sequential {
        /** Creates the provider. */
        public Reserved() {
        }

        @Override
        public String id() {
            return Backend.REFERENCE_ID;
        }
    }

    /** First of two providers declaring the same id. */
    public static final class DuplicateA extends Sequential {
        /** Creates the provider. */
        public DuplicateA() {
        }

        @Override
        public String id() {
            return "test-dup";
        }
    }

    /** Second of two providers declaring the same id. */
    public static final class DuplicateB extends Sequential {
        /** Creates the provider. */
        public DuplicateB() {
        }

        @Override
        public String id() {
            return "test-dup";
        }
    }

    private static final class SequentialProgram implements DeviceProgram {
        private final JavaKernel kernel;
        private final int launchSize;
        private final IntBuffer[] device;
        private final boolean corrupt;

        SequentialProgram(JavaKernel kernel, Grid1D grid, int[] lengths, boolean corrupt) {
            this.kernel = kernel;
            this.launchSize = grid.launchSize();
            this.device = Arrays.stream(lengths).mapToObj(IntBuffer::allocate).toArray(IntBuffer[]::new);
            this.corrupt = corrupt;
        }

        @Override
        public void upload(int slot, IntBuffer host) {
            device[slot].copyFrom(host);
        }

        @Override
        public void download(int slot, IntBuffer host) {
            host.copyFrom(device[slot]);
            if (corrupt && host.length() > 0) {
                host.set(0, host.get(0) ^ 1);
            }
        }

        @Override
        public void launch() {
            IntStream.range(0, launchSize).forEach(index -> kernel.run(index, device));
        }

        @Override
        public void close() {
            // heap-managed
        }
    }
}
