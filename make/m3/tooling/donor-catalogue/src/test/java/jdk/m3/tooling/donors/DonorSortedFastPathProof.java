/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.tooling.donors;

import java.io.IOException;
import java.util.Random;
import java.util.TreeMap;

/** Original TreeMap ordering is the oracle; never equate Git byte-order with UTF-16. */
public final class DonorSortedFastPathProof {
    private DonorSortedFastPathProof() {}
    public static void main(String[] args) throws Exception {
        long checks = 0;
        final int size = 32_768;
        // True sorted Git-style ASCII source order; ordered fast-path avoids scratch allocation.
        DonorTreeLanes sorted = new DonorTreeLanes();
        for (int i = 0; i < size; i++) {
            String path = String.format(java.util.Locale.ROOT, "src/java/%08d.java", i);
            sorted.add(path, "100644", "blob", Integer.toString(i));
        }
        sorted.freeze();
        for (int i = 0; i < size; i++) {
            String expect = String.format(java.util.Locale.ROOT, "src/java/%08d.java", i);
            require(expect.equals(sorted.path(i)), "ordered fast-path: " + i);
            require(Integer.toString(i).equals(sorted.objectId(i)), "metadata: " + i);
            checks += 2;
        }
        // Java/UTF-16 compares the high surrogate of rocket BEFORE BMP U+E100.
        // Git UTF-8 byte ordering instead sorts BMP E100 before rocket.
        DonorTreeLanes unicode = new DonorTreeLanes();
        unicode.add("src/\uE100/X.java", "100644", "blob", "bmp");
        unicode.add("src/\uD83D\uDE80/Y.java", "100755", "blob", "rocket");
        unicode.freeze();
        require(unicode.path(0).endsWith("Y.java"), "UTF-16 fallback order");
        require(unicode.objectId(0).equals("rocket"), "UTF-16 fallback identity");
        checks += 2;
        // Reverse order and shuffled arbitrary metadata must match independent TreeMap.
        for (int variant = 0; variant < 48; variant++) {
            TreeMap<String, String> reference = new TreeMap<>();
            DonorTreeLanes lanes = new DonorTreeLanes();
            int n=97+variant*17;
            int[] order = new int[n];
            for (int i=0;i<n;i++) order[i]=i;
            if ((variant&1)==0) {
                for(int i=0;i<n/2;i++) {int v=order[i];order[i]=order[n-i-1];order[n-i-1]=v;}
            } else {
                Random rng=new Random(20261102L+variant);
                for(int i=n-1;i>0;i--) {int j=rng.nextInt(i+1);int v=order[i];order[i]=order[j];order[j]=v;}
            }
            for(int i:order) {
                String path=(i%11==0?"src/\uD83D\uDE80/":i%13==0?"src/\uE100/":"src/java/")
                    +String.format(java.util.Locale.ROOT,"%08d.java",i);
                String metadata="oid-"+i;
                lanes.add(path,"100644","blob",metadata);
                reference.put(path,metadata);
            }
            lanes.freeze();
            int i=0;
            for(var e:reference.entrySet()) {
                require(e.getKey().equals(lanes.path(i)), "random order="+variant);
                require(e.getValue().equals(lanes.objectId(i)), "random OID="+variant);
                checks += 2;
                i++;
            }
        }
        // Reordered duplicate names must throw rather than admit a silent hash collision.
        DonorTreeLanes duplicate=new DonorTreeLanes();
        duplicate.add("b", "100644", "blob", "b");
        duplicate.add("a", "100644", "blob", "a");
        duplicate.add("b", "100644", "blob", "second-b");
        try { duplicate.freeze(); throw new AssertionError("duplicate accepted"); }
        catch(IOException expected) { checks++; }
        System.out.println("M3_DONOR_SORTED_FASTPATH_PASS\tchecks="+checks+"\tordered=32768\tvariants=48");
    }
    private static void require(boolean ok,String message) {if(!ok)throw new AssertionError(message);}
}
