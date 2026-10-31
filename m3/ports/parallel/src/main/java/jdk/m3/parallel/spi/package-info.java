/* SPDX-License-Identifier: Apache-2.0 */

/**
 * Backend service-provider interface of the M3 parallel API.
 *
 * <p>A backend implements {@link jdk.m3.parallel.spi.BackendProvider} and is registered with
 * {@code provides jdk.m3.parallel.spi.BackendProvider with ...} (module path) or
 * {@code META-INF/services/jdk.m3.parallel.spi.BackendProvider} (class path). Its
 * {@link jdk.m3.parallel.spi.DeviceProgram} maps one-to-one onto the native entry points: allocate (prepare),
 * upload, launch, download and close.</p>
 */
package jdk.m3.parallel.spi;
