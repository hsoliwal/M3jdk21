/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

/** Constants with bodies (anonymous subclasses) and an empty toString. */
enum Body {
    PLUS {
        @Override public String toString() {
            return "+";
        }
    },
    MINUS,
    BLANK {
        @Override public String toString() {
            return "";
        }
    }
}
