import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * ClassPoolDiff : CLASS_POOL_DIFFERENTIAL v1. The donor MIndexClassPool family (com.synexia.indexstring.type, with
 * stubs for its text pool: spelling-equal interning, dense handles, Java String hash) against the relocated family
 * (jdk.internal.m3.indexstring.type over jdk.m3.intern.M3InternSpace). Each round interns random class specs (kinds,
 * names, packages, modules, modifiers, super/interface handles that may be stale or invalid, field/method/constructor/
 * record-component members, duplicates) into both pools and compares every observable: refusals, handles, find(),
 * names, kinds, modifiers, super/interfaces, members, the five 64-bit hashes and SHA-256 of classes and members,
 * snapshot topology SHA-256, depth, subtype lists and topological order, size and retained counts.
 */
public final class ClassPoolDiff {
    static final String[] NAMES = {"java.lang.Object", "a.B", "a.C", "a.B$Inner", "x.y.Z", "Q", "a.B", "é.Ж"};
    static final String[] PACKAGES = {"", "a", "x.y", "java.lang"};
    static final String[] MODULES = {"", "java.base", "m3.app"};
    static final String[] MEMBER_NAMES = {"f", "g", "run", "value", "x", "<init>"};
    static final String[] DESCRIPTORS = {"I", "J", "()V", "(I)I", "Ljava/lang/String;", "(Ljava/lang/Object;)Z"};
    static long dd = 3, rd = 3, checks, mismatches;

    static void eq(String what, Object d, Object r) {
        checks++;
        String a = String.valueOf(d), b = String.valueOf(r);
        dd = dd * 31 + a.hashCode();
        rd = rd * 31 + b.hashCode();
        if (!a.equals(b) && mismatches++ < 10) System.out.println("MISMATCH " + what + " donor=" + a + " relocated=" + b);
    }

    static String run(Supplier<Object> s) {
        try {
            Object v = s.get();
            if (v instanceof int[] x) return Arrays.toString(x);
            if (v instanceof byte[] x) return HexFormat.of().formatHex(x);
            return String.valueOf(v);
        } catch (RuntimeException e) {
            return "THROW " + e.getClass().getSimpleName();
        }
    }

    public static void main(String[] args) {
        SplittableRandom r = new SplittableRandom(Long.parseLong(args[0]));
        int rounds = Integer.parseInt(args[1]);
        for (int round = 0; round < rounds; round++) {
            var dp = new com.synexia.indexstring.type.MIndexClassPool(new com.synexia.indexstring.MatIndexStringPool(null), 1);
            var rp = new jdk.internal.m3.indexstring.type.MIndexClassPool(1);
            int classes = 1 + r.nextInt(12);
            List<Integer> handles = new ArrayList<>();
            for (int c = 0; c < classes; c++) {
                int kind = r.nextInt(5), ns = r.nextInt(3), mods = r.nextInt(1 << 12);
                String name = NAMES[r.nextInt(NAMES.length)], pkg = PACKAGES[r.nextInt(PACKAGES.length)], mod = MODULES[r.nextInt(MODULES.length)];
                int sup = handles.isEmpty() || r.nextInt(3) == 0 ? (r.nextInt(10) == 0 ? 99 : 0) : handles.get(r.nextInt(handles.size()));
                int[] ifs = new int[handles.isEmpty() ? 0 : r.nextInt(Math.min(3, handles.size() + 1))];
                for (int i = 0; i < ifs.length; i++) ifs[i] = handles.get(r.nextInt(handles.size()));
                int members = r.nextInt(5);
                Object[] dm = new Object[members], rm = new Object[members];
                for (int m = 0; m < members; m++) {
                    String mn = MEMBER_NAMES[r.nextInt(MEMBER_NAMES.length)], desc = DESCRIPTORS[r.nextInt(DESCRIPTORS.length)];
                    int mm = r.nextInt(1 << 10), mk = r.nextInt(4);
                    dm[m] = switch (mk) {
                        case 0 -> com.synexia.indexstring.type.MIndexClassMemberSpec.field(mn, desc, mm);
                        case 1 -> com.synexia.indexstring.type.MIndexClassMemberSpec.method(mn, desc, mm);
                        case 2 -> com.synexia.indexstring.type.MIndexClassMemberSpec.constructor(desc, mm);
                        default -> com.synexia.indexstring.type.MIndexClassMemberSpec.recordComponent(mn, desc);
                    };
                    rm[m] = switch (mk) {
                        case 0 -> jdk.internal.m3.indexstring.type.MIndexClassMemberSpec.field(mn, desc, mm);
                        case 1 -> jdk.internal.m3.indexstring.type.MIndexClassMemberSpec.method(mn, desc, mm);
                        case 2 -> jdk.internal.m3.indexstring.type.MIndexClassMemberSpec.constructor(desc, mm);
                        default -> jdk.internal.m3.indexstring.type.MIndexClassMemberSpec.recordComponent(mn, desc);
                    };
                }
                String dres = run(() -> {
                    var spec = new com.synexia.indexstring.type.MIndexClassSpec(ns, com.synexia.indexstring.type.MIndexClassKind.values()[kind],
                            name, pkg, mod, mods, sup, ifs, Arrays.copyOf(dm, dm.length, com.synexia.indexstring.type.MIndexClassMemberSpec[].class));
                    return dp.intern(spec).handle();
                });
                String rres = run(() -> {
                    var spec = new jdk.internal.m3.indexstring.type.MIndexClassSpec(ns, jdk.internal.m3.indexstring.type.MIndexClassKind.values()[kind],
                            name, pkg, mod, mods, sup, ifs, Arrays.copyOf(rm, rm.length, jdk.internal.m3.indexstring.type.MIndexClassMemberSpec[].class));
                    return rp.intern(spec).handle();
                });
                eq("intern", dres, rres);
                if (!dres.startsWith("THROW") && !handles.contains(Integer.parseInt(dres))) handles.add(Integer.parseInt(dres));
                eq("find", run(() -> dp.find(ns, mod, name)), run(() -> rp.find(ns, mod, name)));
            }
            eq("size", dp.size() + ":" + dp.retainedMemberCount() + ":" + dp.retainedInterfaceHandleCount(),
                    rp.size() + ":" + rp.retainedMemberCount() + ":" + rp.retainedInterfaceHandleCount());
            for (int h : handles) {
                var dc = dp.value(h);
                var rc = rp.value(h);
                eq("class", List.of(dc.namespaceId(), dc.kind().name(), dc.modifiers(), dc.binaryName(), dc.packageName(), dc.moduleName(),
                                dc.hasSuperClass(), Arrays.toString(dc.interfaceHandles()), dc.memberCount()),
                        List.of(rc.namespaceId(), rc.kind().name(), rc.modifiers(), rc.binaryName(), rc.packageName(), rc.moduleName(),
                                rc.hasSuperClass(), Arrays.toString(rc.interfaceHandles()), rc.memberCount()));
                eq("hashes", List.of(dc.identityHash64(), dc.exactHash64(), dc.structuralHash64(), dc.logicHash64(), dc.simHash64(),
                                HexFormat.of().formatHex(dc.sha256())),
                        List.of(rc.identityHash64(), rc.exactHash64(), rc.structuralHash64(), rc.logicHash64(), rc.simHash64(),
                                HexFormat.of().formatHex(rc.sha256())));
                for (int m = 0; m < dc.memberCount(); m++) {
                    var a = dc.memberAt(m);
                    var b = rc.memberAt(m);
                    eq("member", List.of(a.kind().name(), a.name(), a.descriptor(), a.modifiers(), a.exactHash64(), a.structuralHash64(), a.logicHash64(), a.simHash64()),
                            List.of(b.kind().name(), b.name(), b.descriptor(), b.modifiers(), b.exactHash64(), b.structuralHash64(), b.logicHash64(), b.simHash64()));
                }
            }
            var ds = dp.snapshot();
            var rs = rp.snapshot();
            eq("snapshot", ds.classCount() + ":" + ds.edgeCount() + ":" + ds.topologySha256() + ":" + Arrays.toString(ds.topologicalHandles()),
                    rs.classCount() + ":" + rs.edgeCount() + ":" + rs.topologySha256() + ":" + Arrays.toString(rs.topologicalHandles()));
            for (int h : handles) {
                eq("depth", run(() -> ds.depth(h)) + run(() -> ds.directSubtypeHandles(h)), run(() -> rs.depth(h)) + run(() -> rs.directSubtypeHandles(h)));
            }
        }
        System.out.println("CLASS_POOL_DIFFERENTIAL rounds=" + rounds + " checks=" + checks + " mismatches=" + mismatches
                + " digest donor=" + Long.toHexString(dd) + " relocated=" + Long.toHexString(rd));
        if (mismatches != 0) System.exit(1);
    }
}
