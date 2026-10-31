// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package m3proof;

/** Parsed against actual compiled receiving owners; this fixture executes no resource callbacks. */
final class DependencyCustody {
    Object use(java.util.Collection<Object> source,
               jdk.m3.collections.M3ProgressMonitor progress,
               org.eclipse.core.resources.IResourceDelta delta) {
        jdk.m3.eclipse.runtime.M3EclipseProgressMonitor.borrowed(progress).worked(1);
        jdk.m3.eclipse.framework.M3EclipseResourceDeltaSnapshot.capture(delta, 32).size();
        return jdk.m3.collections.M3FilteredViews.filter(source, value -> true);
    }
}
