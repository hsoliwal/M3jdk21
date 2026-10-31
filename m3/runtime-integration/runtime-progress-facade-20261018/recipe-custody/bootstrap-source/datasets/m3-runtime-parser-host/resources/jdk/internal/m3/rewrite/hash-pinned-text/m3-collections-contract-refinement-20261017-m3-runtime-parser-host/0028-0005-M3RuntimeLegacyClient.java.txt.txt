/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Existing Runtime calls and native descriptors remain usable
 * @run main/othervm M3RuntimeLegacyClient
 */
import java.lang.reflect.Modifier;

/** External recipe proof freezes the stock-Java21-compiled bytes of this class. */
public final class M3RuntimeLegacyClient {
    private static int checks;

    private static void check(boolean truth, String reason) {
        checks++;
        if (!truth) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        check(runtime == Runtime.getRuntime(), "existing Runtime singleton");
        check(runtime.availableProcessors() > 0, "native availableProcessors");
        check(runtime.freeMemory() >= 0, "native freeMemory");
        check(runtime.totalMemory() > 0, "native totalMemory");
        check(runtime.maxMemory() > 0, "native maxMemory");
        check(Runtime.version().feature() == 21, "existing version feature");
        Runtime.Version parsed = Runtime.Version.parse("21.0.1+12");
        check(parsed.feature() == 21 && parsed.update() == 1, "existing version parser");
        check(parsed.build().orElseThrow() == 12, "existing optional version build");
        check(parsed.compareTo(Runtime.Version.parse("21.0.1+11")) > 0,
                "existing version comparison");
        check(Modifier.isPrivate(Runtime.class.getDeclaredConstructor().getModifiers()),
                "existing private constructor");
        Thread hook = new Thread(() -> { }, "m3-runtime-legacy-hook");
        runtime.addShutdownHook(hook);
        check(runtime.removeShutdownHook(hook), "existing shutdown hook removal");
        check(!runtime.removeShutdownHook(hook), "existing absent hook removal");
        System.out.println("M3_RUNTIME_LEGACY_CLIENT_PASS checks=" + checks);
    }
}
