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
package org.apache.commons.codec.digest;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * A pure-Java implementation of the CRC32 checksum that uses
 * the same polynomial as the built-in native CRC32.
 * <p>
 * This is to avoid the JNI overhead for certain uses of checksumming
 * where many small pieces of data are checksummed in succession.
 * </p>
 * <p>
 * The current version is ~10x to 1.8x as fast as Sun's native
 * java.util.zip.CRC32 in Java 1.6
 * </p>
 * <p>
 * Copied from Apache Hadoop 2.6.3.
 * The code agrees with the following file in the 2.6.3 tag:
 * https://gitbox.apache.org/repos/asf?p=hadoop.git;a=blob_plain;
 * f=hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/PureJavaCrc32.java;
 * hb=2120de588b92b9f22b1cc4188761d6a8c61aa778
 * </p>
 * <p>
 * This class is Not ThreadSafe
 * </p>
 *
 * <p>
 * M3JDK (CHECKSUMS_TO_JDK v1): java.base computes the checksum; this class delegates to
 * {@code java.util.zip.CRC32}, which HotSpot compiles to the processor's CRC instructions.
 * </p>
 * @see java.util.zip.CRC32
 * @since 1.11
 */
public class PureJavaCrc32 implements Checksum {

    //FLLDCIM: M3 - CHECKSUMS_TO_JDK v1: java.base computes the checksum, the JDK's one implementation
    private final CRC32 crc = new CRC32();

    /** Create a new PureJavaCrc32 object. */
    public PureJavaCrc32() {
        resetCrc();
    }

    @Override
    public long getValue() {
        return crc.getValue();
    }

    @Override
    public void reset() {
        resetCrc();
    }

    // called by ctor, so must not be overrideable
    private void resetCrc() {
        crc.reset();
    }

    @Override
    public void update(final byte[] b, final int offset, final int len) {
        crc.update(b, offset, len);
    }

    @Override
    final public void update(final int b) {
        crc.update(b);
    }

}
