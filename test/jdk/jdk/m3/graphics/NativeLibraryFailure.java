/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */

/*
 * @test
 * @summary A native initialization failure must not leave the library reported as loaded
 * @modules jdk.m3.graphics
 * @run main/othervm/native -Xcheck:jni NativeLibraryFailure
 */

import java.io.File;
import jdk.m3.graphics.impl.M3Library;

public class NativeLibraryFailure {
    public static void main(String[] args) {
        File library = new File(System.getProperty("test.nativepath"),
                System.mapLibraryName("NativeBootstrapFailure"));
        if (M3Library._loaded) throw new AssertionError("already loaded");
        try {
            M3Library._loadFromFile(library);
            throw new AssertionError("initialization unexpectedly succeeded");
        } catch (UnsatisfiedLinkError expected) {
            if (!expected.getMessage().equals("intentional bootstrap failure")) throw expected;
        }
        if (M3Library._loaded) throw new AssertionError("failed initialization reported as loaded");
        System.out.println("PASS: failed native initialization rolls back loader state");
    }
}
