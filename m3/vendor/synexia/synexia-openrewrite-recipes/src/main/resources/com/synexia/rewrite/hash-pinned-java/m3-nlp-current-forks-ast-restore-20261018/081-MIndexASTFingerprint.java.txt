// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Domain-separated Merkle metadata. Exact tuples, never this digest, authorize interning. */
final class MIndexASTFingerprint {
    private MIndexASTFingerprint() { }

    static String of(MIndexASTStore store, int kind, int[] attributes, int[] roles, int[] children) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            var out = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), hash));
            out.writeLong(0x4d49445841535431L);
            text(out, store.symbol(kind));
            out.writeInt(attributes.length / 2);
            for (int id : attributes) { text(out, store.symbol(id)); }
            out.writeInt(children.length);
            for (int i = 0; i < children.length; i++) {
                text(out, store.symbol(roles[i]));
                out.write(HexFormat.of().parseHex(store.node(children[i]).fingerprint));
            }
            out.flush();
            return HexFormat.of().formatHex(hash.digest());
        } catch (IOException | NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    static void text(DataOutputStream out, CharSequence text) throws IOException {
        out.writeInt(text.length());
        for (int i = 0; i < text.length(); i++) { out.writeChar(text.charAt(i)); }
    }
}
