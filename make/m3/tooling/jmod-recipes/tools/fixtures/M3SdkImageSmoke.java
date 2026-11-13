// SPDX-License-Identifier: Apache-2.0
/**
 * Java-21-compatible live API smoke for an M3SDK jlink image.
 *
 * <p>Uses reflection so compilation itself never borrows a donor JAR. The runtime must actually
 * resolve the named donor modules from the candidate image; class/module presence alone is not
 * sufficient, and both real library methods must return the expected results.</p>
 */
public final class M3SdkImageSmoke {
    private M3SdkImageSmoke() {}

    public static void main(String[] arguments) throws ReflectiveOperationException {
        requireBootModule("re2j");
        requireBootModule("com.google.common");

        Class<?> patternType = Class.forName("com.google.re2j.Pattern");
        Object pattern =
                patternType.getMethod("compile", String.class)
                        .invoke(null, "^M3SDK[0-9]+$");
        Object matcher =
                patternType.getMethod("matcher", CharSequence.class)
                        .invoke(pattern, "M3SDK21");
        Object matched = matcher.getClass().getMethod("matches").invoke(matcher);
        if (!Boolean.TRUE.equals(matched)) {
            throw new AssertionError("RE2J_RUNTIME_BEHAVIOR_DRIFT");
        }

        Class<?> immutableList = Class.forName("com.google.common.collect.ImmutableList");
        Object list = immutableList.getMethod("of", Object.class)
                .invoke(null, "m3-sdk");
        Object first = list.getClass().getMethod("get", int.class).invoke(list, 0);
        if (!"m3-sdk".equals(first)) {
            throw new AssertionError("GUAVA_RUNTIME_BEHAVIOR_DRIFT");
        }

        if (Runtime.version().feature() != 21) {
            throw new AssertionError("NOT_JAVA_21");
        }
        System.out.println("M3_SDK_VENDOR_API_SMOKE_PASS");
    }

    private static void requireBootModule(String name) {
        if (ModuleLayer.boot().findModule(name).isEmpty()) {
            throw new AssertionError("MISSING_LINKED_MODULE:" + name);
        }
    }
}
