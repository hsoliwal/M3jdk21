/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Check IPv6 usability and the native error-string status/buffer contract
 * @requires (os.family == "linux")
 * @run main/othervm/native -Djava.net.preferIPv4Stack=false AndroidRuntimeChecks
 * @run main/othervm/native -Djava.net.preferIPv4Stack=true AndroidRuntimeChecks ipv4
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.file.Files;
import java.nio.file.Path;

public class AndroidRuntimeChecks {
    static {
        System.loadLibrary("AndroidRuntimeChecks");
    }

    private static native boolean hasIpv6Loopback();
    private static native int errorStringFailures();

    public static void main(String[] args) throws Exception {
        int failures = errorStringFailures();
        if (failures != 0) {
            throw new AssertionError("Native error-string contract failure mask: " + failures);
        }
        checkProcessError();
        if (args.length == 1 && args[0].equals("ipv4")) {
            try (DatagramChannel unexpected = DatagramChannel.open(StandardProtocolFamily.INET6)) {
                throw new AssertionError("Forced IPv4 allowed an IPv6 channel: " + unexpected);
            } catch (UnsupportedOperationException expected) {
                // The explicit Java property continues to disable IPv6.
            }
            roundTrip(StandardProtocolFamily.INET, "127.0.0.1");
            System.out.println("Forced IPv4 and native error-string checks passed");
            return;
        }
        if (!hasIpv6Loopback()) {
            System.out.println("IPv6 loopback is unavailable; error-string checks passed");
            return;
        }
        roundTrip(StandardProtocolFamily.INET6, "::1");
        System.out.println("Native error-string and Java IPv6 loopback checks passed");
    }

    private static void checkProcessError() throws Exception {
        Path directory = Files.createTempDirectory("native-error-");
        try {
            new ProcessBuilder(directory.resolve("missing-executable").toString()).start();
            throw new AssertionError("Starting a missing executable succeeded");
        } catch (IOException expected) {
            String message = expected.getMessage();
            Throwable cause = expected.getCause();
            if (message == null || !message.contains("missing-executable") ||
                cause == null || cause.getMessage() == null || cause.getMessage().isBlank()) {
                throw new AssertionError("Missing executable lost its native diagnostic", expected);
            }
        } finally {
            Files.delete(directory);
        }
    }

    private static void roundTrip(StandardProtocolFamily family, String literal) throws Exception {
        InetAddress loopback = InetAddress.getByName(literal);
        try (DatagramChannel channel = DatagramChannel.open(family);
             Selector selector = Selector.open()) {
            channel.bind(new InetSocketAddress(loopback, 0));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            byte[] payload = {11, 23, 37, 41};
            int sent = channel.send(ByteBuffer.wrap(payload), channel.getLocalAddress());
            if (sent != payload.length) {
                throw new AssertionError("Incomplete loopback send: " + sent);
            }
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (selector.select(100) == 0) {
                if (System.nanoTime() >= deadline) {
                    throw new AssertionError("Loopback receive timed out for " + family);
                }
            }
            ByteBuffer received = ByteBuffer.allocate(payload.length + 1);
            if (channel.receive(received) == null || received.position() != payload.length) {
                throw new AssertionError("Missing or incorrectly sized loopback datagram");
            }
            for (int i = 0; i < payload.length; i++) {
                if (received.get(i) != payload[i]) {
                    throw new AssertionError("Loopback payload mismatch at " + i);
                }
            }
        }
    }
}
