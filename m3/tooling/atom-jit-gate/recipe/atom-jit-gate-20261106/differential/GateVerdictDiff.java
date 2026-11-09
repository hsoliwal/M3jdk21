// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Oracle 4 (serial gate half): the donor M3OpenJdkAtomJitPatternGate (its plan built by the donor planner) and the
// relocated gate (its Plan built from the donor plan's root and target order) review the same seeded receipt ledgers;
// every verdict (review fields, or the refusal class and message, of the receipt constructor or of review) must be
// identical. Reflective, so no file in the naming scope names the donor package (it comes from the relocation map).
// args: <donor package> <relocated package> <donor recipe class> <donor crate pom> <scenarios> <seed> <transcript dir>
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public final class GateVerdictDiff {
    private static final String[] PATHS = {
        "src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java",
        "m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
        "src/java.base/share/classes/java/util/ArrayList.java",
        "m3/arrays/src/main/native/m3_arrays_jni.c",
        "m3/collections/src/main/java/jdk/m3/collections/LongHashSet.java"};
    private static final String NONE = "-";

    private record Side(Class<?> gate, Class<?> receipt, Class<?> phase, Class<?> status, Method review) { }

    private record Row(String path, String atom, String envelope, int phase, int status, String proof, String jdk,
            String behavior, boolean compiled) { }

    public static void main(String[] a) throws Exception {
        String dpkg = a[0], rpkg = a[1], recipe = a[2], pom = a[3];
        int scenarios = Integer.parseInt(a[4]);
        Random random = new Random(Long.parseLong(a[5]));
        Class<?> planner = Class.forName(dpkg + ".M3UniversalRecipeTaskInvariant");
        Class<?> dTarget = Class.forName(planner.getName() + "$Target");
        Class<?> dLane = Class.forName(planner.getName() + "$SourceLane");
        Class<?> dPlan = Class.forName(planner.getName() + "$Plan");
        Method plan = planner.getMethod("plan", String.class, String.class, String.class, String.class, String.class,
                String.class, int.class, List.class);
        Class<?> rGate = Class.forName(rpkg + ".M3OpenJdkAtomJitPatternGate");
        Class<?> rTarget = Class.forName(rGate.getName() + "$Target");
        Class<?> rLane = Class.forName(rGate.getName() + "$SourceLane");
        Class<?> rPlan = Class.forName(rGate.getName() + "$Plan");
        Side donor = side(dpkg, dPlan);
        Side relocated = side(rpkg, rPlan);
        Constructor<?> dTargetNew = dTarget.getConstructors()[0];
        Constructor<?> rTargetNew = rTarget.getConstructors()[0];
        Constructor<?> rPlanNew = rPlan.getConstructors()[0];
        int phases = donor.phase().getEnumConstants().length;
        List<String> donorLines = new ArrayList<>(), relocatedLines = new ArrayList<>();
        Map<String, Integer> kinds = new TreeMap<>();
        int different = 0;
        for (int s = 0; s < scenarios; s++) {
            int n = 1 + random.nextInt(4);
            List<Object> targets = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                boolean jni = random.nextInt(3) == 0;
                targets.add(dTargetNew.newInstance(PATHS[random.nextInt(PATHS.length)], "atom_" + s + "_" + i,
                        sha(random), dLane.getEnumConstants()[jni ? 1 : 0]));
            }
            Object donorPlan = plan.invoke(null, "m3jdk21.primitive.mask", "bit_manipulation", recipe, pom,
                    sha(random), sha(random), 4, targets);
            List<?> ordered = (List<?>) call(donorPlan, "targets");
            List<Object> relocatedTargets = new ArrayList<>();
            List<Object[]> spec = new ArrayList<>();
            for (Object t : ordered) {
                Object lane = call(t, "lane");
                relocatedTargets.add(rTargetNew.newInstance(call(t, "logicalPath"), call(t, "atomId"),
                        call(t, "beforeEnvelopeRoot"), rLane.getEnumConstants()[((Enum<?>) lane).ordinal()]));
                spec.add(new Object[] {call(t, "logicalPath"), call(t, "atomId"), call(t, "beforeEnvelopeRoot"),
                        ((Enum<?>) lane).ordinal() == 1});
            }
            Object relocatedPlan = rPlanNew.newInstance(call(donorPlan, "root"), relocatedTargets);
            List<Row> rows = complete(spec, phases);
            mutate(rows, random, phases, spec);
            String dv = verdict(donor, donorPlan, rows);
            String rv = verdict(relocated, relocatedPlan, rows);
            donorLines.add(s + "\t" + dv);
            relocatedLines.add(s + "\t" + rv);
            if (!dv.equals(rv)) different++;
            kinds.merge(dv.substring(0, dv.indexOf(' ')), 1, Integer::sum);
        }
        Path out = Files.createDirectories(Path.of(a[6]));
        Files.write(out.resolve("donor-gate-verdicts.txt"), donorLines, StandardCharsets.UTF_8);
        Files.write(out.resolve("relocated-gate-verdicts.txt"), relocatedLines, StandardCharsets.UTF_8);
        System.out.println("GATE_VERDICTS scenarios=" + scenarios + " different=" + different + " kinds=" + kinds
                + " donor_sha256=" + digest(out.resolve("donor-gate-verdicts.txt"))
                + " relocated_sha256=" + digest(out.resolve("relocated-gate-verdicts.txt")));
        System.exit(different == 0 ? 0 : 1);
    }

    private static Side side(String pkg, Class<?> planType) throws Exception {
        Class<?> gate = Class.forName(pkg + ".M3OpenJdkAtomJitPatternGate");
        return new Side(gate, Class.forName(gate.getName() + "$Receipt"), Class.forName(gate.getName() + "$Phase"),
                Class.forName(gate.getName() + "$Status"), gate.getMethod("review", planType, List.class));
    }

    /** The donor test's complete ledger: PASS everywhere, JNI_PARITY N/A for Java atoms, runtime witnesses set. */
    private static List<Row> complete(List<Object[]> spec, int phases) {
        List<Row> rows = new ArrayList<>();
        for (Object[] t : spec) {
            for (int p = 0; p < phases; p++) {
                boolean jniPhase = p == 4;
                int status = jniPhase && !(Boolean) t[3] ? 4 : 0;
                boolean runtime = status == 0 && (p == 2 || p == 3 || p == 4);
                rows.add(new Row((String) t[0], (String) t[1], (String) t[2], p, status, rep('c'),
                        runtime ? rep('d') : NONE, runtime ? (jniPhase ? rep('9') : rep('e')) : NONE, p == 3));
            }
        }
        return rows;
    }

    private static void mutate(List<Row> rows, Random random, int phases, List<Object[]> spec) {
        int mutations = random.nextInt(4);
        for (int m = 0; m < mutations && !rows.isEmpty(); m++) {
            int at = random.nextInt(rows.size());
            Row r = rows.get(at);
            switch (random.nextInt(9)) {
                case 0 -> { // status change with consistent witnesses
                    int status = random.nextInt(5);
                    boolean runtime = status == 0 && (r.phase() == 2 || r.phase() == 3 || r.phase() == 4);
                    String proof = status == 0 || status == 4 ? rep('c') : NONE;
                    rows.set(at, new Row(r.path(), r.atom(), r.envelope(), r.phase(), status, proof,
                            runtime ? rep('d') : NONE, runtime ? rep('e') : NONE, status == 0 && r.phase() == 3));
                }
                case 1 -> rows.set(at, new Row(r.path(), r.atom(), r.envelope(), r.phase(), r.status(), r.proof(),
                        r.jdk().equals(NONE) ? NONE : rep("0123456789abcdef".charAt(random.nextInt(16))), r.behavior(),
                        r.compiled()));
                case 2 -> rows.set(at, new Row(r.path(), r.atom(), r.envelope(), r.phase(), r.status(), r.proof(),
                        r.jdk(), r.behavior().equals(NONE) ? NONE : rep("0123456789abcdef".charAt(random.nextInt(16))),
                        r.compiled()));
                case 3 -> rows.remove(at);
                case 4 -> java.util.Collections.swap(rows, at, random.nextInt(rows.size()));
                case 5 -> rows.set(at, new Row(r.path(), r.atom(), sha(random), r.phase(), r.status(), r.proof(),
                        r.jdk(), r.behavior(), r.compiled()));
                case 6 -> rows.set(at, new Row(r.path(), r.atom(), r.envelope(), r.phase(), r.status(), r.proof(),
                        r.jdk(), r.behavior(), !r.compiled()));
                case 7 -> { // a whole serial suffix of one target held (the donor JIT HOLD scenario)
                    int first = at - at % phases;
                    int from = Math.min(rows.size(), first + 2 + random.nextInt(phases - 2));
                    for (int i = from; i < Math.min(rows.size(), first + phases); i++) {
                        Row h = rows.get(i);
                        rows.set(i, new Row(h.path(), h.atom(), h.envelope(), h.phase(), i == from ? 1 : 2, NONE,
                                NONE, NONE, false));
                    }
                }
                default -> rows.set(at, new Row(r.path(), r.atom(), r.envelope(), r.phase(), r.status(), NONE,
                        r.jdk(), r.behavior(), r.compiled()));
            }
        }
    }

    private static String verdict(Side side, Object plan, List<Row> rows) throws Exception {
        List<Object> receipts = new ArrayList<>();
        Constructor<?> make = side.receipt().getConstructors()[0];
        for (Row r : rows) {
            try {
                receipts.add(make.newInstance(r.path(), r.atom(), r.envelope(),
                        side.phase().getEnumConstants()[r.phase()], side.status().getEnumConstants()[r.status()],
                        r.proof(), r.jdk(), r.behavior(), r.compiled()));
            } catch (InvocationTargetException refused) {
                return "RECEIPT_REFUSED " + refused.getCause().getClass().getSimpleName() + ": "
                        + refused.getCause().getMessage();
            }
        }
        try {
            Object review = side.review().invoke(null, plan, receipts);
            return "REVIEW atoms=" + call(review, "atoms") + " patternizeReady=" + call(review, "patternizeReady")
                    + " structurallyQualified=" + call(review, "structurallyQualified") + " blockers="
                    + call(review, "blockers") + " root=" + call(review, "root") + " promotionAuthority="
                    + call(review, "promotionAuthority") + " independentlyVerified="
                    + call(review, "independentlyVerified");
        } catch (InvocationTargetException refused) {
            return "REVIEW_REFUSED " + refused.getCause().getClass().getSimpleName() + ": "
                    + refused.getCause().getMessage();
        }
    }

    private static Object call(Object target, String name) throws Exception {
        Method method = target.getClass().getMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static String rep(char digit) {
        return String.valueOf(digit).repeat(64);
    }

    private static String sha(Random random) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 64; i++) out.append("0123456789abcdef".charAt(random.nextInt(16)));
        return out.toString();
    }

    private static String digest(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
