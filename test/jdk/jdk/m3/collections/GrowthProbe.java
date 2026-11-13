// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
public final class GrowthProbe {
    private GrowthProbe() { }
    public static void main(String[] args) {
        var m = new LongOrderStatisticMultiset();
        for (int i=0;i<17;i++) m.add(i);
        if (m.count(16) != 1 || m.size() != 17 || m.rank(16) != 16 || m.select(16) != 16)
            throw new AssertionError("lost array growth insertion: size="+m.size()+" key16="+m.count(16)+" rank16="+m.rank(16)+" selected="+m.select(16));
        System.out.println("PASS 17th growth child published to live primitive index array");
    }
}
