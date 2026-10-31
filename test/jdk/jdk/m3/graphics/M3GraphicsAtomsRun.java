/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * One side's run of one case of the jdk.m3.graphics atom tests (M3GraphicsAtoms): the receiver and arguments are
 * materialized on the side, the member is called with System.out and System.err captured, and the outcome records the
 * result or the throwable, the result's aliasing of the receiver and arguments, the receiver and arguments after the
 * call, the atom's public static mutable fields and the console output, with ISO-8601 timestamps masked (M3Log prints
 * one per line); every other character of the console compares.
 */
final class M3GraphicsAtomsRun {
    /** What one side did for one case. */
    record Outcome(Object result, Throwable thrown, Object receiver, Object[] args, int aliases, String console,
            List<Object> statics) {}

    private static final Pattern CONSOLE_TIME = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?::\\d{2}(?:[.,]\\d+)?)?(?:Z|[+-]\\d{2}:?\\d{2})");
    private static final String TIME = "<time>";
    private static final int FIRST_ARG_BIT = 1;

    private M3GraphicsAtomsRun() {}

    /** Runs one case on one side with System.out and System.err captured. */
    static Outcome run(M3GraphicsAtomsOracle.Side side, Class<?> type, M3GraphicsAtoms.Call c,
        M3GraphicsAtomsValues.Logical recv, List<M3GraphicsAtomsValues.Logical> args) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = System.out;
        PrintStream err = System.err;
        PrintStream capture = new PrintStream(buf, true, StandardCharsets.UTF_8);
        System.setOut(capture);
        System.setErr(capture);
        try {
            return execute(side, type, c, recv, args, buf);
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

    private static Outcome execute(M3GraphicsAtomsOracle.Side side, Class<?> type, M3GraphicsAtoms.Call c,
        M3GraphicsAtomsValues.Logical recv, List<M3GraphicsAtomsValues.Logical> logical,
        ByteArrayOutputStream buf) {
        Object receiver = null;
        Object[] args = new Object[logical.size()];
        try {
            receiver = M3GraphicsAtomsValues.materialize(recv, side);
            for (int p = 0; p < args.length; p++) {
                args[p] = M3GraphicsAtomsValues.materialize(logical.get(p), side);
            }
            Object result = M3GraphicsAtomsOracle.call(c.targets().get(side), c.kind(), receiver, args);
            return new Outcome(result, null, receiver, args, aliases(result, receiver, args), console(buf),
                    statics(type));
        } catch (Throwable t) {
            Throwable cause = t instanceof InvocationTargetException ite ? ite.getCause() : t;
            return new Outcome(null, cause, receiver, args, 0, console(buf), statics(type));
        }
    }

    private static List<Object> statics(Class<?> type) {
        return Arrays.stream(type.getFields()).filter(M3GraphicsAtomsRun::isMutableStatic)
                .sorted(Comparator.comparing(Field::getName)).map(M3GraphicsAtomsRun::read).toList();
    }

    private static boolean isMutableStatic(Field f) {
        return Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers());
    }

    private static Object read(Field f) {
        try {
            return f.get(null);
        } catch (IllegalAccessException e) {
            return e.getClass().getName();
        }
    }

    private static String console(ByteArrayOutputStream buf) {
        return CONSOLE_TIME.matcher(buf.toString(StandardCharsets.UTF_8)).replaceAll(TIME);
    }

    private static int aliases(Object result, Object receiver, Object[] args) {
        int bits = result != null && result == receiver ? 1 : 0;
        for (int p = 0; p < args.length; p++) {
            bits |= result != null && result == args[p] ? FIRST_ARG_BIT << (p + 1) : 0;
        }
        return bits;
    }
}
