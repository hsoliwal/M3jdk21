/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Check dense-array Apache stack against 60,000 JDK list/stack operations
 * @modules jdk.m3.collections/jdk.m3.collections.apache
 * @compile ArrayStackContract.java
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.m3.collections.apache=ALL-UNNAMED ArrayStackContract
 */
public final class M3ArrayStackJTReg {
    private M3ArrayStackJTReg() {
    }
}
