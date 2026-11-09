/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Some portions of this file Copyright (c) 2004-2006 Intel Corporation
 * and licensed under the BSD license.
 */
package org.apache.commons.codec.digest;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.zip.Checksum;

/**
 * A pure-Java implementation of the CRC32 checksum that uses the CRC32-C polynomial, the same polynomial used by iSCSI and implemented on many Intel chipsets
 * supporting SSE 4.2.
 *
 * <p>
 * Copied from Apache Hadoop 2.3.6: https://gitbox.apache.org/repos/asf?p=hadoop.git;a=blob_plain;
 * f=hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PureJavaCrc32C.java; hb=2120de588b92b9f22b1cc4188761d6a8c61aa778
 * </p>
 * <p>
 * This class is Not ThreadSafe
 * </p>
 *
 * <p>
 * M3JDK (CHECKSUMS_TO_JDK v1): java.base computes the checksum; this class delegates to
 * {@code java.util.zip.CRC32C}, which HotSpot compiles to the processor's CRC instructions.
 * </p>
 * @since 1.11
 */
public class PureJavaCrc32C implements Checksum {

    //FLLDCIM: M3 - CHECKSUMS_TO_JDK v1: java.base computes the checksum, the JDK's one implementation
    private static final MethodHandle NEW_CRC32C = crc32cConstructor();

    private final Checksum crc = newCrc32c();

    /** Create a new PureJavaCrc32 object. */
    public PureJavaCrc32C() {
        reset();
    }

    @Override
    public long getValue() {
        return crc.getValue();
    }

    @Override
    public void reset() {
        crc.reset();
    }

    @Override
    public void update(final byte[] b, int off, int len) {
        crc.update(b, off, len);
    }

    @Override
    final public void update(final int b) {
        crc.update(b);
    }

    // java.util.zip.CRC32C is Java 9 API and the donor compiles with --release 8 (Guava's Hashing does the same).
    private static MethodHandle crc32cConstructor() {
        try {
            return MethodHandles.publicLookup()
                    .findConstructor(Class.forName("java.util.zip.CRC32C"), MethodType.methodType(void.class))
                    .asType(MethodType.methodType(Checksum.class));
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Checksum newCrc32c() {
        try {
            return (Checksum) NEW_CRC32C.invokeExact();
        } catch (final RuntimeException | Error e) {
            throw e;
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }
    }

}
