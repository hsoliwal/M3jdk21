// SPDX-License-Identifier: Apache-2.0
/** M3 viewport adapter seam (row host, events, painter) and thin toolkit glue; the views live in com.m3.collections. */
module com.m3.viewport {
    requires transitive com.m3.collections;
    requires transitive java.desktop;
    exports com.m3.viewport;
    exports com.m3.viewport.awt;
}
