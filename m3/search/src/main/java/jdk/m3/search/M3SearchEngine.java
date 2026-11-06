// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/SearchEngine.java (blob 430253d54dc0) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;

/** Provider boundary for Java or optional native precomputed byte-search implementations. */
public interface M3SearchEngine {
    String name();
    int priority();
    boolean available();
    M3CompiledSearch compile(byte[] needle);

    /** Progress-aware compatibility entrypoint. */
    default M3CompiledSearch compile(byte[] needle, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        M3CompiledSearch result = compile(needle);
        try {
            checked.checkCanceled();
            return result;
        } catch (RuntimeException | Error failure) {
            if (result != null) {
                try {
                    result.close();
                } catch (RuntimeException | Error releaseFailure) {
                    if (releaseFailure != failure) failure.addSuppressed(releaseFailure);
                }
            }
            throw failure;
        }
    }
}
