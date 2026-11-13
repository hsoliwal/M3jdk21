/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
import demo.pkg.Outer;
public final class JniJoinRuntime {
    public static void main(String[] args) {
        if (Outer.count(new int[]{1, 2, 3}) != 6) throw new AssertionError("JNI array sum");
        if (Outer.ping() != 7) throw new AssertionError("JNI ping");
        if (new Outer.Inner().do_Thing(11) != 13) throw new AssertionError("JNI nested escaped name");
        if (new Outer().overloaded(8) != 10) throw new AssertionError("JNI overloaded int long symbol");
        if (new Outer().overloaded(new byte[]{1, 2, 3, 4}) != 4) {
            throw new AssertionError("JNI overloaded byte array long symbol");
        }
        System.out.println("M3_JNI_EXPORT_RUNTIME_PASS|checks=5");
    }
}
