/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

/**
 * Source-owned ABI contract for the optional Synexia code-text packed JNI lane.
 *
 * <p>This class records the boundary and validates target-side shape. It does not load native
 * code or make JNI semantic authority: the host-Java receiver remains the exact fallback.</p>
 */
final class M3CodeTextNativeContract {
    static final String SOURCE_REPOSITORY = "hsoliwal/com.synexia";
    static final int SOURCE_PULL_REQUEST = 10124;
    static final String SOURCE_HEAD =
            "4a9897b9327c26e348ff78fd40731f5bfdcbe473";
    static final String SOURCE_PATH =
            "synexia-indexstring/native/src/mindex_code_text_signal_jni.c";
    static final String SOURCE_BLOB =
            "152b0c2b4062e5a83afafcdc06106ab874cf40e5";
    static final String COMPANION_SIGNAL_BLOB =
            "27b6f34ab44b00e82ec814660f249f0b6bfd7cbd";
    static final String COMPANION_HEADER_BLOB =
            "6b1e4a5716aacfa71c33aedf5ed249f4af4a325d";

    static final String JNI_OWNER =
            "com.synexia.indexstring.JniMIndexCodeTextSignalBatch";
    static final String JNI_METHOD = "nativeAnalyzeRange";
    static final String JNI_DESCRIPTOR = "([C[I[III)[J";
    static final String JNI_EXPORT =
            "Java_com_synexia_indexstring_JniMIndexCodeTextSignalBatch_nativeAnalyzeRange";

    private M3CodeTextNativeContract() {}

    static boolean accepts(char[] units, int[] offsets, int[] lengths, int fromRow, int toRow) {
        if (units == null || offsets == null || lengths == null
                || offsets.length != lengths.length
                || fromRow < 0 || toRow < fromRow || toRow > offsets.length) {
            return false;
        }
        for (int row = fromRow; row < toRow; row++) {
            int offset = offsets[row];
            int length = lengths[row];
            if (offset < 0 || length < 0 || (long) offset + length > units.length) {
                return false;
            }
        }
        return true;
    }

    static long transferBytes(int utf16Units, int rows) {
        if (utf16Units < 0 || rows < 0) {
            throw new IllegalArgumentException("negative code-text JNI geometry");
        }
        return Math.addExact(
                Math.addExact(2L * utf16Units, 4L * rows),
                Math.addExact(4L * rows, 8L * rows));
    }
}
