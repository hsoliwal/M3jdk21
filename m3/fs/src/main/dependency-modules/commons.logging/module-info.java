// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/** Descriptor adaptation for the unchanged, SHA-256-pinned Apache Commons Logging 1.2 JAR. */
module commons.logging {
    requires java.logging;
    exports org.apache.commons.logging;
    exports org.apache.commons.logging.impl;
}
