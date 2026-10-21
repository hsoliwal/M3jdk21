// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;
// SPDX-License-Identifier: Apache-2.0

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class M3ASTHierarchyContract {
    private static int checks;
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
        checks++;
    }
    private static void reject(Class<? extends Throwable> type, Runnable action, String reason) {
        try { action.run(); }
        catch (Throwable actual) { check(type.isInstance(actual), reason+" got "+actual); return; }
        throw new AssertionError("accepted invalid "+reason);
    }
    private static String sha(int i) { return "%064x".formatted((long) i + 1); }
    private static M3ASTHierarchy.FileFact fact(int i) {
        String project = "project" + (i % 4);
        String module = "module" + ((i / 4) % 5);
        String pkg = "com.m3.family" + ((i / 20) % 13);
        String path = "src/main/java/Family" + i + ".java";
        int len = 1 + i % 17;
        int[] handles = new int[len];
        byte[] masks = new byte[len];
        for (int j = 0; j < len; j++) { handles[j] = 1 + j * 3 + i * 19; masks[j] = (byte) (i + j * 17); }
        return new M3ASTHierarchy.FileFact(project,module,pkg,path,sha(i),sha(i+10000),
                1 << (i % 13),handles,masks);
    }
    private static void coverage(M3ASTHierarchy result) {
        for (int lvl=0;lvl<6;lvl++) {
            var level = result.level(M3ASTHierarchy.Level.values()[lvl]);
            check(level.size()>0,"nonempty L"+lvl);
            int position=0;
            for (int i=0;i<level.size();i++) {
                check(level.firstChild(i)==position,"contiguous L"+lvl);
                check(level.childCount(i)>0,"nonempty child range L"+lvl);
                check(level.root(i).matches("[0-9a-f]{64}"),"hash L"+lvl);
                check(level.occurrenceCount(i)>0,"occurrence count L"+lvl);
                for (int j=0;j<level.childCount(i);j++) {
                    check(level.childOrdinal(i,j)==position+j,"indexed child L"+lvl);
                }
                position+=level.childCount(i);
            }
            int expected = lvl==0 ? result.totalOccurrences()
                    : result.level(M3ASTHierarchy.Level.values()[lvl-1]).size();
            check(position==expected,"parent child coverage L"+lvl);
        }
    }
    public static void main(String[] args) {
        int count = args.length==0?2000:Integer.parseInt(args[0]);
        var facts = new ArrayList<M3ASTHierarchy.FileFact>(count);
        int expectedAtoms=0;
        for(int i=0;i<count;i++){facts.add(fact(i));expectedAtoms += 1+i%17;}
        var original=M3ASTHierarchy.build("hsoliwal/com.synexia",facts);
        check(original.totalOccurrences()==expectedAtoms,"total atom count");
        check(original.totalFiles()==count,"file count");
        if (count >= 260) {
            check(original.level(M3ASTHierarchy.Level.L2_PACKAGE).size()==260,"package fan-in");
            check(original.level(M3ASTHierarchy.Level.L3_MODULE).size()==20,"module fan-in");
            check(original.level(M3ASTHierarchy.Level.L4_PROJECT).size()==4,"project fan-in");
        }
        if (count>=13)original.requireFamilies(M3ASTHierarchy.ALL_COLLECTION_FAMILIES);
        coverage(original);
        String baseline=original.rootSha256();
        check(baseline.matches("[0-9a-f]{64}"),"root hash");
        var rnd=new Random(0x53a0L);
        for(int n=0;n<32;n++) {
            Collections.shuffle(facts,rnd);
            var replay=M3ASTHierarchy.build("hsoliwal/com.synexia",facts);
            check(replay.rootSha256().equals(baseline),"permutation root");
            check(replay.totalOccurrences()==expectedAtoms,"permutation count");
            check(replay.familyMask()==original.familyMask(),"permutation families");
        }
        for(int i=0;i<5;i++) {
            var first = facts.get(i);
            int[] h=first.atomHandles(); byte[] s=first.atomStateMasks();
            check(h.length==s.length,"aligned getters");
            h[0]=123456; s[0]=127;
            check(first.atomHandles()[0]!=123456,"not reached");
        }
        // Test input arrays cannot become later mutable ownership of retained coordinates.
        int[] handles={1,2,3};byte[] flags={1,0,-1};
        var sealed=new M3ASTHierarchy.FileFact("project","module","pkg","File.java",
                sha(6000),sha(6001),M3ASTHierarchy.MAP,handles,flags);
        handles[0]=999;flags[0]=0;
        check(sealed.atomHandles()[0]==1,"defensive input atom handles");
        check(sealed.atomStateMasks()[0]==1,"defensive input masks");
        sealed.atomHandles()[0]=1000;sealed.atomStateMasks()[0]=42;
        check(sealed.atomHandles()[0]==1,"defensive output atom handles");
        var one=M3ASTHierarchy.build("r",List.of(sealed));
        check(one.totalOccurrences()==3,"one-file hierarchy");
        check(one.atomHandle(0)==1,"flat atom coordinates");
        check(one.stateMask(2)==255,"unsigned mask view");
        check(one.level(M3ASTHierarchy.Level.L2_PACKAGE).size()==1,"one package");
        check(one.level(M3ASTHierarchy.Level.L3_MODULE).size()==1,"one module");
        check(one.level(M3ASTHierarchy.Level.L4_PROJECT).size()==1,"one project");
        check(one.level(M3ASTHierarchy.Level.L5_REPOSITORY).size()==1,"one repository");
        reject(IllegalStateException.class,()->one.requireFamilies(M3ASTHierarchy.SET),"coverage omitted");
        reject(IllegalArgumentException.class,()->M3ASTHierarchy.build("r", List.of(sealed,sealed)),"duplicate file");
        reject(IllegalArgumentException.class,()->M3ASTHierarchy.build("r",List.of()),"empty repo");
        reject(IllegalArgumentException.class,()->M3ASTHierarchy.build("",List.of(sealed)),"blank repo");
        reject(IllegalArgumentException.class,()->new M3ASTHierarchy.FileFact("p","m","pkg","../escape",sha(1),sha(2),1,new int[]{1},new byte[]{1}),"traversal");
        reject(IllegalArgumentException.class,()->new M3ASTHierarchy.FileFact("p","m","pkg","foo\\bar",sha(1),sha(2),1,new int[]{1},new byte[]{1}),"windows path");
        reject(IllegalArgumentException.class,()->new M3ASTHierarchy.FileFact("p","m","pkg","foo",sha(1),sha(2),1,new int[]{1},new byte[]{1,2}),"misaligned flags");
        reject(IllegalArgumentException.class,()->new M3ASTHierarchy.FileFact("p","m","pkg","foo",sha(1),sha(2),1,new int[]{0},new byte[]{1}),"invalid atom handle");
        reject(IllegalArgumentException.class,()->new M3ASTHierarchy.FileFact("p","m","pkg","foo",sha(1),sha(2),0,new int[]{1},new byte[]{1}),"missing family classification");
        reject(IllegalArgumentException.class,()->new M3ASTHierarchy.FileFact("p","m","pkg","foo",sha(1),sha(2),1<<22,new int[]{1},new byte[]{1}),"unknown family mask");
        reject(IllegalArgumentException.class,()->new M3ASTHierarchy.FileFact("p","m","pkg","foo","not-a-hash",sha(2),1,new int[]{1},new byte[]{1}),"bad source seal");
        // Tests a single atom state or receipt drift changes the content-addressed root.
        var changed = new M3ASTHierarchy.FileFact("project","module","pkg","File.java",
                sha(6000),sha(6001),M3ASTHierarchy.MAP,new int[]{1,2,3},new byte[]{2,0,-1});
        check(!one.rootSha256().equals(M3ASTHierarchy.build("r",List.of(changed)).rootSha256()),"state drift hash");
        var changedReceipt = new M3ASTHierarchy.FileFact("project","module","pkg","File.java",
                sha(6000),sha(7001),M3ASTHierarchy.MAP,new int[]{1,2,3},new byte[]{1,0,-1});
        check(!one.rootSha256().equals(M3ASTHierarchy.build("r",List.of(changedReceipt)).rootSha256()),"receipt drift hash");
        // Validate the retained layout never has an entry, tree node or per-atom object array.
        for (Class<?> clazz:List.of(M3ASTHierarchy.class, M3ASTHierarchy.Plane.class)) {
            for(Field f:clazz.getDeclaredFields()) {
                if(Modifier.isStatic(f.getModifiers()))continue;
                String type=f.getType().getTypeName();
                check(!type.contains("Entry")&&!type.contains("Node")&&!type.contains("Map"),"no entry/node fields");
            }
        }
        System.out.println("PASS HierarchyContract checks="+checks+" files="+count+" atoms="+expectedAtoms+" root="+baseline);
    }
}
