// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

/** HTTP admission policy; production checks run before request-body parsing. */
public final class ChromeMlAdmissionPolicy {
    interface Lease extends AutoCloseable {
        Lease NOOP = () -> { };

        @Override
        void close();
    }

    private final boolean production;
    private final boolean corsAllowed;
    private final String authority;
    private final byte[] expectedAuthorization;
    private final Semaphore permits;

    private ChromeMlAdmissionPolicy(
            boolean production,
            boolean corsAllowed,
            String authority,
            byte[] expectedAuthorization,
            Semaphore permits) {
        this.production = production;
        this.corsAllowed = corsAllowed;
        this.authority = Objects.requireNonNullElse(authority, "");
        this.expectedAuthorization =
                expectedAuthorization == null ? new byte[0] : expectedAuthorization.clone();
        this.permits = permits;
    }

    public static ChromeMlAdmissionPolicy embedding() {
        return new ChromeMlAdmissionPolicy(false, true, "", null, null);
    }

    public static ChromeMlAdmissionPolicy production(
            String bearerToken,
            InetSocketAddress boundAddress,
            int maxConcurrentRequests) {
        String token = requireStrongToken(bearerToken);
        InetSocketAddress bound = Objects.requireNonNull(boundAddress, "boundAddress");
        if (bound.getAddress() == null || !bound.getAddress().isLoopbackAddress()) {
            throw new IllegalArgumentException("numeric loopback address required");
        }
        if (maxConcurrentRequests < 1 || maxConcurrentRequests > 1024) {
            throw new IllegalArgumentException("maxConcurrentRequests");
        }
        String host = bound.getAddress().getHostAddress();
        String authority = host.indexOf(':') >= 0
                ? "[" + host + "]:" + bound.getPort()
                : host + ":" + bound.getPort();
        byte[] expected =
                ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        return new ChromeMlAdmissionPolicy(
                true,
                false,
                authority,
                expected,
                new Semaphore(maxConcurrentRequests, true));
    }

    Lease admit(HttpExchange exchange) throws ChromeMlRequestException {
        Objects.requireNonNull(exchange, "exchange");
        if (!production) {
            return Lease.NOOP;
        }

        if (exchange.getRemoteAddress().getAddress() == null
                || !exchange.getRemoteAddress().getAddress().isLoopbackAddress()) {
            throw new ChromeMlRequestException(403, "LOOPBACK_CALLER_REQUIRED");
        }
        List<String> hosts = exchange.getRequestHeaders().get("Host");
        if (hosts == null || hosts.size() != 1 || !matchesAuthority(hosts.getFirst())) {
            throw new ChromeMlRequestException(400, "Host header does not match the bound loopback authority");
        }

        List<String> origins = exchange.getRequestHeaders().get("Origin");
        if (origins != null) {
            throw new ChromeMlRequestException(403, "Browser Origin requests are not admitted");
        }

        List<String> authorization = exchange.getRequestHeaders().get("Authorization");
        if (authorization == null || authorization.size() != 1 || authorization.getFirst().length() > 4103) {
            throw new ChromeMlRequestException(401, "Bearer authentication required");
        }
        byte[] actual = authorization.getFirst().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedAuthorization, actual)) {
            throw new ChromeMlRequestException(401, "Bearer authentication failed");
        }

        if (!permits.tryAcquire()) {
            throw new ChromeMlRequestException(429, "Concurrent request limit reached");
        }
        return permits::release;
    }

    private boolean matchesAuthority(String supplied) {
        return authority.equals(supplied) || authority.startsWith("[0:0:0:0:0:0:0:1]:")
                && authority.replace("[0:0:0:0:0:0:0:1]", "[::1]").equals(supplied);
    }

    boolean corsAllowed() {
        return corsAllowed;
    }

    public boolean production() {
        return production;
    }

    public String authority() {
        return authority;
    }

    static String requireStrongToken(String value) {
        String token = Objects.requireNonNull(value, "bearerToken");
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        if (token.isBlank()
                || bytes.length < 32
                || bytes.length > 4096
                || token.chars().anyMatch(character -> character < 33 || character > 126)) {
            throw new IllegalArgumentException("strong bearer token of at least 32 UTF-8 bytes required");
        }
        return token;
    }
}
