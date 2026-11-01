/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/**
 * The M3 graphics owner: the 2D drawing API M3jdk carries itself (absorbed from HumbleUI Skija 0.143.18 and HumbleUI
 * Types 0.2.0, Apache-2.0; see legal/skija.md). Native implementation: Skia, through the module's JNI library.
 *
 * @moduleGraph
 */
module jdk.m3.graphics {
    exports jdk.m3.graphics;
    exports jdk.m3.graphics.impl;
    exports jdk.m3.graphics.paragraph;
    exports jdk.m3.graphics.resources;
    exports jdk.m3.graphics.shaper;
    exports jdk.m3.graphics.skottie;
    exports jdk.m3.graphics.sksg;
    exports jdk.m3.graphics.svg;
}
