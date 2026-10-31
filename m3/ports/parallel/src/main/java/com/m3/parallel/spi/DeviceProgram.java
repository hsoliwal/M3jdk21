/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel.spi;

import com.m3.parallel.IntBuffer;

/**
 * Device-resident state of one plan: one storage slot per binding plus a loaded kernel.
 *
 * <p>The caller ({@link com.m3.parallel.Plan}) serializes every call, validates slot indices and buffer lengths, and
 * applies the {@link com.m3.parallel.Transfer} policy. Implementations only move bytes and launch.</p>
 */
public interface DeviceProgram extends AutoCloseable {

    /**
     * Copies {@code host} into device slot {@code slot}.
     *
     * @param slot binding index
     * @param host host buffer with the slot's length
     */
    void upload(int slot, IntBuffer host);

    /**
     * Copies device slot {@code slot} into {@code host}.
     *
     * @param slot binding index
     * @param host host buffer with the slot's length
     */
    void download(int slot, IntBuffer host);

    /**
     * Launches the kernel over the whole grid and returns when every work-item has finished.
     */
    void launch();

    /**
     * Releases device storage. Called once.
     */
    @Override
    void close();
}
