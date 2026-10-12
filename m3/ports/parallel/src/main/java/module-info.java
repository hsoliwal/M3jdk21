/* SPDX-License-Identifier: Apache-2.0 */

/**
 * M3 data-parallel launch API.
 *
 * <p>{@code com.m3.parallel} holds the user-facing types (backend selection, off-heap int buffers, kernel ids,
 * one-dimensional grids, transfer policies, immutable plans, results and verified receipts). The pure-Java CPU
 * reference backend is always present. Native backends are discovered through
 * {@link com.m3.parallel.spi.BackendProvider} with {@link java.util.ServiceLoader}.</p>
 */
module m3.parallel {
    exports com.m3.parallel;
    exports com.m3.parallel.spi;

    uses com.m3.parallel.spi.BackendProvider;
}
