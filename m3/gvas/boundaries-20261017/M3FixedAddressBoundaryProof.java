/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Path;
import java.security.MessageDigestSpi;
import java.security.Provider;
import java.security.Security;
import java.util.Locale;

/** Isolated-process failure/lifetime oracle for the original fixed-address owner. */
public final class M3FixedAddressBoundaryProof {
    private static final long BASE = M3FixedAddressMapping.DEFAULT_BASE;
    private static final RuntimeException MARKER = new IllegalStateException("digest failure marker");
    private static final Error ERROR_MARKER = new AssertionError("digest error marker");
    private static boolean throwError;
    private static int checks;

    private M3FixedAddressBoundaryProof() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try { operation.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "unexpected " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }
    public static final class ThrowingDigest extends MessageDigestSpi {
        public ThrowingDigest() { }
        private static void rejected() {
            if (throwError) throw ERROR_MARKER;
            throw MARKER;
        }
        @Override protected void engineUpdate(byte input) { rejected(); }
        @Override protected void engineUpdate(byte[] input, int offset, int length) { rejected(); }
        @Override protected byte[] engineDigest() { rejected(); return new byte[0]; }
        @Override protected void engineReset() { }
    }
    private static final class FailingProvider extends Provider {
        private static final long serialVersionUID = 1L;
        FailingProvider() {
            super("M3IsolatedFailureOracle", "1.0", "test-only provider; removed in finally");
            put("MessageDigest.SHA-256", ThrowingDigest.class.getName());
        }
    }
    private static void rejectedProvider(Path file, Path library, String digest) throws IOException {
        for (boolean error : new boolean[] {false, true}) {
            throwError = error;
            Provider provider = new FailingProvider();
            check(Security.insertProviderAt(provider, 1) == 1, "isolated provider registration");
            try {
                M3FixedAddressMapping.open(file, BASE, digest, library);
                throw new AssertionError("failing digest was admitted");
            } catch (RuntimeException | Error failure) {
                check(failure == (error ? ERROR_MARKER : MARKER), "preserve failure identity");
            } finally {
                Security.removeProvider(provider.getName());
            }
        }
        // A leaked failed admission occupies BASE, so this retry is a real unmap oracle.
        verify(M3FixedAddressMapping.open(file, BASE, digest, library), file, digest);
    }
    private static void verify(M3FixedAddressMapping.Image image, Path file, String digest)
            throws IOException {
        check(image.address() == BASE, "exact virtual address");
        check(image.file().equals(file.toRealPath()), "canonical file");
        check(image.sha256().equals(digest.toLowerCase(Locale.ROOT)), "digest");
        check(image.byteLength() == 4096, "length");
        check(image.addressOfOffset(128) == BASE + 128, "same base plus offset");
        ByteBuffer view = image.readOnlyBuffer();
        check(view.isDirect() && view.isReadOnly(), "native read-only view");
        check(view.get(128) == 'Z' && view.get(129) == '6', "canonical payload");
        view.position(10);
        check(image.readOnlyBuffer().position() == 0, "independent view cursor");
        expect(ReadOnlyBufferException.class, () -> view.put(0, (byte) 4));
        expect(IndexOutOfBoundsException.class, () -> image.addressOfOffset(-1));
        expect(IndexOutOfBoundsException.class, () -> image.addressOfOffset(4096));
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("mode library image sha256");
        String mode = args[0];
        Path library = Path.of(args[1]);
        Path file = Path.of(args[2]);
        String digest = args[3];
        switch (mode) {
            case "cleanup" -> rejectedProvider(file, library, digest);
            case "digest" -> {
                try {
                    M3FixedAddressMapping.open(file, BASE, "0".repeat(64), library);
                    throw new AssertionError("wrong SHA admitted");
                } catch (IOException expected) { checks++; }
                verify(M3FixedAddressMapping.open(file, BASE, digest, library), file, digest);
            }
            case "collision" -> {
                M3FixedAddressMapping.reserveCollisionForProof(BASE, 4096, library);
                try {
                    M3FixedAddressMapping.open(file, BASE, digest, library);
                    throw new AssertionError("occupied slot overwritten");
                } catch (IOException expected) { checks++; }
            }
            case "child", "unicode" -> {
                var image = M3FixedAddressMapping.open(file, BASE, digest, library);
                verify(image, file, digest);
                check(M3FixedAddressMapping.open(file, BASE, digest.toUpperCase(Locale.ROOT), library)
                        == image, "same process canonical reuse");
                try {
                    M3FixedAddressMapping.open(file, BASE, "0".repeat(64), library);
                    throw new AssertionError("published image replaced");
                } catch (IllegalStateException expected) { checks++; }
                if (mode.equals("child")) {
                    System.out.println("M3_GVAS_BOUNDARY_READY base=0x" + Long.toHexString(image.address()));
                    System.out.flush();
                    if (System.in.read() == -1) throw new IOException("parent disappeared");
                    verify(image, file, digest);
                }
            }
            default -> throw new IllegalArgumentException("unknown mode " + mode);
        }
        System.out.println("M3_GVAS_BOUNDARY_PASS mode=" + mode + " checks=" + checks);
    }
}
