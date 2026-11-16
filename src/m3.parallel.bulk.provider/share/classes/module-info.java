/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */

/** Bridges reviewed operations from the JDK bulk scheduler to interchangeable parallel engines. */
module m3.parallel.bulk.provider {
    requires transitive m3.parallel;

    exports jdk.m3.parallel.bulk;
}
