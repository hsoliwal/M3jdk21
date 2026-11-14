// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileTextShadow.java blob e46b21eecdd61dbe73212cd92cddb09c942d7db4 (retained PR #1619 head; parent 0cd0944136c1 blob 098a71a071a6a58ded5068548051c078bbbd0787 https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileTextShadow.java); recipe m3/file/qualification/retained-port-20261107 RESTORE_EXACT

/** Collects decoded chars; for small files and tests. */
public final class M3FileTextShadow implements M3FileCharShadow {
    private final StringBuilder text = new StringBuilder();

    M3FileTextShadow() {
    }

    @Override
    public void update(char[] chars, int offset, int length) {
        text.append(chars, offset, length);
    }

    public String text() {
        return text.toString();
    }
}
