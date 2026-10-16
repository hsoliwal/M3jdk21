/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Internal M3 representation selection has exact UTF-16 geometry and bounded primitive metadata
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3StringRepresentationPlanTest
 */
import java.lang.reflect.Method;
import java.util.Random;

public class M3StringRepresentationPlanTest {
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("java.lang.M3StringRepresentationPlan");
        Method select = type.getDeclaredMethod("select", boolean[].class);
        select.setAccessible(true);
        Method collapse = type.getDeclaredMethod("collapseRange",
                int.class, int.class, int.class, int.class);
        collapse.setAccessible(true);
        Random random = new Random(20261016L);
        long checks = 0;
        for (int n = 0; n < 256; n++) {
            for (int trial = 0; trial < 20; trial++) {
                boolean[] flags = new boolean[n];
                int count = 0, runs = 0;
                boolean prior = false;
                for (int i = 0; i < flags.length; i++) {
                    flags[i] = random.nextBoolean();
                    if (flags[i]) {
                        count++;
                        if (!prior) runs++;
                    }
                    prior = flags[i];
                }
                Object plan = select.invoke(null, (Object) flags);
                Class<?> planType = plan.getClass();
                Method visibleLength = planType.getDeclaredMethod("visibleLength");
                Method runCount = planType.getDeclaredMethod("runs");
                visibleLength.setAccessible(true);
                runCount.setAccessible(true);
                check(((Integer) visibleLength.invoke(plan)) == count);
                check(((Integer) runCount.invoke(plan)) == runs);
                checks += 2;
            }
        }
        long range = (Long) collapse.invoke(null, 100, 200, 50, 75);
        check((int) (range >>> 32) == 150 && (int) range == 25);
        System.out.println("M3_REPRESENTATION_PLAN_PASS checks=" + (checks + 1));
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("M3 representation geometry mismatch");
    }
}
