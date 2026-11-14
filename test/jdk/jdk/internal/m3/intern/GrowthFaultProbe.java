// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.lang.reflect.Field;
import java.util.Arrays;

/** Runs against throw-instrumented *temporary source*, never a production class. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-failure-atomic-growth-20261030/src/test/java/com/synexia/m3/classspace/M3GrowthFaultProbe.java blob 39b2d45b0b97; recipe make/m3/intern/recipe/class-space-20261107/relocate.py v1
public final class GrowthFaultProbe {
    private GrowthFaultProbe() { }

    public static void main(String[] args) throws Exception {
        boolean lanes = args.length == 1 && args[0].equals("lanes");
        boolean buckets = args.length == 1 && args[0].equals("buckets");
        if (!lanes && !buckets) throw new IllegalArgumentException("lanes or buckets required");
        ClassSpace space = new ClassSpace(65536);
        int threshold = lanes ? 16 : 12;
        for (int i = 0; i < threshold; i++) require(space.intern(atom(i)) == i, "initial ID");
        int count = space.imageCount(), retained = space.retainedByteCount();
        int[] widths = columnWidths(space);
        String flag = "m3.testFault." + args[0];
        System.setProperty(flag, "true");
        try {
            space.intern(atom(threshold));
            throw new AssertionError("missing injected OOME");
        } catch (OutOfMemoryError expected) {
            require(expected.getMessage().contains("synthetic"), "wrong injected error");
        } finally {
            System.clearProperty(flag);
        }
        require(space.imageCount() == count, "rehash failure occurred after publishing a new atom");
        require(space.retainedByteCount() == retained, "failed growth changed retained bytes");
        require(Arrays.equals(widths, columnWidths(space)),
                "failed allocation published one column but not the other columns");
        for (int i = 0; i < threshold; i++) require(Arrays.equals(space.copy(i), atom(i)), "lost atom");
        require(space.intern(atom(threshold)) == threshold, "retry failed or changed ordinal");
        require(Arrays.equals(space.copy(threshold), atom(threshold)), "retry lost bytes");
        System.out.println("FAULT_REPLAY_PASS " + args[0]);
    }

    private static int[] columnWidths(ClassSpace space) throws ReflectiveOperationException {
        return new int[]{width(space, "offsets"), width(space, "lengths"),
                width(space, "next"), width(space, "digests") / 32};
    }

    private static int width(ClassSpace space, String fieldName) throws ReflectiveOperationException {
        Field field = ClassSpace.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object lane = field.get(space);
        return lane instanceof int[] ints ? ints.length : ((byte[]) lane).length;
    }

    private static byte[] atom(int id) {
        return new byte[]{(byte) id, (byte) (id >>> 8), 5, 9};
    }

    private static void require(boolean truth, String why) {
        if (!truth) throw new AssertionError(why);
    }
}
