// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.util.Arrays;

/**
 * Smoke main of the relocated class-metadata pool (recipe classpool-20261107): a small hierarchy whose names, members,
 * interning and topology can be checked by hand. The donor differential is recipe/classpool-20261107/ClassPoolDiff.java.
 */
public final class MIndexClassPoolSmokeMain {
    private MIndexClassPoolSmokeMain() { }

    public static void main(String[] args) {
        MIndexClassPool pool = new MIndexClassPool(1);
        MIndexClass object = pool.intern(new MIndexClassSpec(0, MIndexClassKind.CLASS, "java.lang.Object", "java.lang",
                "java.base", 1, 0, new int[0], new MIndexClassMemberSpec[0]));
        MIndexClass runnable = pool.intern(new MIndexClassSpec(0, MIndexClassKind.INTERFACE, "java.lang.Runnable",
                "java.lang", "java.base", 1537, 0, new int[0],
                new MIndexClassMemberSpec[] {MIndexClassMemberSpec.method("run", "()V", 1025)}));
        MIndexClass task = pool.intern(new MIndexClassSpec(0, MIndexClassKind.CLASS, "a.Task", "a", "m3.app", 1,
                object.handle(), new int[] {runnable.handle()},
                new MIndexClassMemberSpec[] {MIndexClassMemberSpec.field("count", "I", 2),
                        MIndexClassMemberSpec.constructor("()V", 1), MIndexClassMemberSpec.method("run", "()V", 1)}));
        MIndexClass again = pool.intern(new MIndexClassSpec(0, MIndexClassKind.CLASS, "a.Task", "a", "m3.app", 1,
                object.handle(), new int[] {runnable.handle()},
                new MIndexClassMemberSpec[] {MIndexClassMemberSpec.field("count", "I", 2),
                        MIndexClassMemberSpec.constructor("()V", 1), MIndexClassMemberSpec.method("run", "()V", 1)}));
        MIndexClassSnapshot snapshot = pool.snapshot();
        check(again.handle() == task.handle() && pool.size() == 3, "canonical re-intern");
        check(pool.find(0, "m3.app", "a.Task").handle() == task.handle(), "find");
        check(task.binaryName().equals("a.Task") && task.superClass().handle() == object.handle(), "names and super");
        check(task.memberCount() == 3 && task.memberAt(2).name().equals("run")
                && task.memberAt(2).descriptor().equals("()V"), "members");
        check(Arrays.equals(task.interfaceHandles(), new int[] {runnable.handle()}), "interfaces");
        check(snapshot.depth(task.handle()) > snapshot.depth(object.handle()), "topology");
        check(pool.retainedTextAtomCount() > 0, "text atoms in the M3InternSpace");
        System.out.println("CLASS_POOL_SMOKE_PASS classes=" + pool.size() + " members=" + pool.retainedMemberCount()
                + " textAtoms=" + pool.retainedTextAtomCount() + " topology=" + snapshot.topologySha256().substring(0, 12));
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
