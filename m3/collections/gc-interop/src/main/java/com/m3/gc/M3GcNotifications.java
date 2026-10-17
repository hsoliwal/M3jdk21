// SPDX-License-Identifier: Apache-2.0
package com.m3.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

/**
 * Optional Java-management GC notification adapter. A notification only
 * increments an epoch: it NEVER evicts, allocates resource state, retains
 * Java object roots, or calls the collector from its callback.
 * Does not depend on com.sun.management classes or any particular collector.
 */
public final class M3GcNotifications {
    public static final String GC_NOTIFICATION_TYPE = "com.sun.management.gc.notification";
    private static final AtomicLong EPOCH = new AtomicLong();
    private static final NotificationListener LISTENER = (notification, ignored) -> onNotification(notification);
    private static final List<NotificationEmitter> SUBSCRIPTIONS = new ArrayList<>();

    /** Register once with supported collector beans; unsupported environments stay conservative. */
    public static synchronized boolean enable() {
        if (!SUBSCRIPTIONS.isEmpty()) return true;
        try {
            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                if (bean instanceof NotificationEmitter emitter) {
                    try {
                        emitter.addNotificationListener(LISTENER, null, null);
                        SUBSCRIPTIONS.add(emitter);
                    } catch (RuntimeException unsupported) {
                        // Some collectors/management environments reject listeners.
                    }
                }
            }
        } catch (RuntimeException unsupported) {
            // No observer is required to preserve ordinary JVM behavior.
        }
        return !SUBSCRIPTIONS.isEmpty();
    }

    public static long epoch() { return EPOCH.get(); }

    // Package-private for deterministic tests; the production callback uses the same path.
    static void onNotification(Notification notification) {
        if (notification != null && GC_NOTIFICATION_TYPE.equals(notification.getType())) {
            EPOCH.incrementAndGet();
        }
    }

    private M3GcNotifications() { }
}
