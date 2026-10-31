// SPDX-License-Identifier: Apache-2.0
package sun.security.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;

/** Fresh-VM contract probe of the exact JDK-8368692 product source, not a replacement helper. */
public final class M3PasswordPolicyCases {
    private static final String POLICY = "jdk.security.password.allowSystemIn";
    private static int checks;

    private M3PasswordPolicyCases() {}

    /** Arguments are ALLOW, DENY or INVALID plus an optional security-property value. */
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("mode [security-value]");
        String mode = args[0];
        if (!mode.equals("ALLOW") && !mode.equals("DENY") && !mode.equals("INVALID")) {
            throw new IllegalArgumentException("mode");
        }
        if (args.length == 2) Security.setProperty(POLICY, args[1]);
        if (System.console() != null) throw new IllegalStateException("requires redirected non-console VM");
        InputStream original = System.in;
        try {
            TrackingInput input = input("fixture\n");
            System.setIn(input);
            if (mode.equals("INVALID")) {
                try {
                    Password.readPassword(input);
                    throw new AssertionError("invalid property accepted");
                } catch (ExceptionInInitializerError expected) {
                    check(expected.getCause() instanceof IllegalArgumentException, "invalid property cause");
                    check(input.available() == 8, "initialization failure consumes no input");
                }
            } else {
                boolean allowed = mode.equals("ALLOW");
                currentStandardInput(allowed);
                streamCorpus();
                // Policy is captured once, not re-read after mutation of either property source.
                System.setProperty(POLICY, allowed ? "false" : "true");
                Security.setProperty(POLICY, allowed ? "false" : "true");
                currentStandardInput(allowed);
            }
        } finally {
            System.setIn(original);
        }
        System.out.println("M3_PASSWORD_POLICY_PASS mode=" + mode + " checks=" + checks);
    }

    private static void currentStandardInput(boolean allowed) throws Exception {
        TrackingInput input = input("fixture\n");
        System.setIn(input);
        if (allowed) {
            check(Arrays.equals("fixture".toCharArray(), Password.readPassword(input)), "allowed stdin");
        } else {
            try {
                Password.readPassword(input);
                throw new AssertionError("disabled standard input accepted");
            } catch (UnsupportedOperationException expected) {
                check(input.available() == 8, "refusal consumes no bytes");
            }
        }
        check(!input.closed, "caller owns standard input");
        TrackingInput echoed = input("echo\n");
        System.setIn(echoed);
        check(Arrays.equals("echo".toCharArray(), Password.readPassword(echoed, true)), "echo unaffected");
        check(!echoed.closed, "caller owns echo input");
    }

    private static void streamCorpus() throws Exception {
        for (String text : new String[] {"", "\n", "\r\n", "a\n", "a\r\n", "a", "ab\rc\n",
                "x".repeat(127) + "\n", "x".repeat(128) + "\n", "x".repeat(129) + "\n",
                "x".repeat(257) + "\n", "\u0000\u00ff\n"}) {
            TrackingInput input = input(text);
            String expected = text.endsWith("\r\n") ? text.substring(0, text.length() - 2)
                    : text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
            char[] actual = Password.readPassword(input);
            check(expected.isEmpty() ? actual == null : Arrays.equals(expected.toCharArray(), actual),
                    "unrelated stream read contract");
            check(!input.closed, "caller owns unrelated stream");
        }
        try {
            Password.readPassword(null);
            throw new AssertionError("null accepted");
        } catch (NullPointerException expected) {
            check(true, "null remains NPE");
        }
    }

    private static TrackingInput input(String text) {
        return new TrackingInput(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        private boolean closed;
        private TrackingInput(byte[] bytes) { super(bytes); }
        @Override public void close() { closed = true; }
    }
}
