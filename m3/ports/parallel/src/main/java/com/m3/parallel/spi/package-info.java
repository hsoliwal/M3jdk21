/* SPDX-License-Identifier: Apache-2.0 */

/**
 * Backend service-provider interface of the M3 parallel API.
 *
 * <p>A backend implements {@link com.m3.parallel.spi.BackendProvider} and is registered with
 * {@code provides com.m3.parallel.spi.BackendProvider with ...} (module path) or
 * {@code META-INF/services/com.m3.parallel.spi.BackendProvider} (class path). Its
 * {@link com.m3.parallel.spi.DeviceProgram} maps one-to-one onto the native entry points: allocate (prepare),
 * upload, launch, download and close.</p>
 */
package com.m3.parallel.spi;
