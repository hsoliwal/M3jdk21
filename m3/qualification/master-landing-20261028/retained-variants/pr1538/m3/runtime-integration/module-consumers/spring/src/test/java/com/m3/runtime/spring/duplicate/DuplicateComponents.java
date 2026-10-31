/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring.duplicate;

import org.springframework.stereotype.Component;
public final class DuplicateComponents {
    private DuplicateComponents() { }
    @Component("collision") public static final class First { }
    @Component("collision") public static final class Second { }
}
