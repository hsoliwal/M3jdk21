// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.util.List;
import java.util.Map;

/** Java21 compiler-backed branch-join proof. No reclamation or JNI assumptions. */
public final class CRefPathCertaintyProof {
    private static int checks;
    private CRefPathCertaintyProof() { }
    private static void check(boolean value,String message){
        checks++; if(!value)throw new AssertionError(message);
    }
    private static CRefPathCertainty.Method method(String source) throws Exception {
        var units=Map.of("F.java",source);
        var references=ReferenceFlow.analyze(units,null);
        var flow=SourceLiveness.analyzeExtended(units,null);
        return CRefPathCertainty.analyze(units,references,flow,null).methods()
                .stream().filter(m->m.signature().contains("#f(")).findFirst().orElseThrow();
    }
    public static void main(String[] args) throws Exception {
        checks=0;
        var direct=method("class F {static void f(){int[] a=new int[2];int[] b=a;b=null;}}");
        check(direct.complete(),"direct alias method compiler-qualified: "+direct.unresolved());
        check(direct.snapshots().size()==3,"three source transitions");
        check(direct.snapshots().get(0).mustLocal()[1]==1,"definite original array");
        check(direct.snapshots().get(1).mustLocal()[1]==2,"local alias doubles");
        check(direct.snapshots().get(2).mustLocal()[1]==1,"null removes one local alias");
        var branch=method("class F {static void f(boolean c){int[] a=new int[2];if(c){a=null;}}}");
        check(branch.complete(),"conditional source admitted: "+branch.unresolved());
        var join=branch.snapshots().getLast();
        check("IF_JOIN".equals(join.phase()),"path join recorded");
        check(join.mayLocal()[1]==1 && join.mustLocal()[1]==0,
                "null on one path removes definite but not possible alias");
        check(!join.singleDefiniteLocalCandidate(1),"conditional alias not unique");
        var both=method("class F {static void f(boolean c){int[] a=new int[2];if(c){a=null;}else{a=null;}}}");
        check(both.complete(),"both branches recognized: "+both.unresolved());
        check(both.snapshots().getLast().mayLocal()[1]==0,"both branches remove alias");
        var same=method("class F {static void f(boolean c){int[] a=new int[2];int[] b=null;if(c){b=a;}else{b=a;}}}");
        check(same.complete(),"same-origin join recognized: "+same.unresolved());
        check(same.snapshots().getLast().mustLocal()[1]==2,"must-alias at both branch successors");
        var alternatives=method("class F {static void f(boolean c){int[] a=new int[2];int[] b=null;if(c){b=a;}else{b=new int[3];}}}");
        check(alternatives.complete(),"distinct branch origins recognized: "+alternatives.unresolved());
        var different=alternatives.snapshots().getLast();
        check(different.mayLocal()[1]==2 && different.mustLocal()[1]==1,"may versus must join");
        check(different.mayLocal()[2]==1 && different.mustLocal()[2]==0,
                "conditional second allocation cannot be guaranteed");
        var external=method("class F {static void f(int[] unknown){int[] a=new int[2];}}");
        check(external.complete(),"external parameter is qualified TOP");
        check(external.snapshots().getFirst().mayLocal()[0]==1
                && !external.snapshots().getFirst().singleDefiniteLocalCandidate(1),
                "unknown input blocks uniqueness candidate");
        var call=method("class F {static void f(){int[] a=new int[2];System.out.println(a);}}");
        check(!call.complete() && call.snapshots().isEmpty(),"call effects refuse entire method");
        var loop=method("class F {static void f(){int[] a=new int[2];while(true){a=null;}}}");
        check(!loop.complete(),"loop effects refused");
        int[] changed=direct.snapshots().getFirst().mustLocal();
        changed[1]=-10;
        check(direct.snapshots().getFirst().mustLocal()[1]==1,"immutable result arrays");
        check(direct.snapshots().stream().noneMatch(CRefPathCertainty.Snapshot::authorizesReclamation),
                "no GC authorization");
        var lexical=method("class F {static void f(){int[] a=new int[2];{int[] b=a;}int[] c=a;}}");
        check(lexical.complete(),"nested lexical block accepted: "+lexical.unresolved());
        check(lexical.snapshots().stream().anyMatch(x ->
                x.phase().equals("SCOPE_EXIT") && x.mustLocal()[1]==1),
                "expired lexical alias must disappear at scope exit");
        check(lexical.snapshots().getLast().mustLocal()[1]==2,
                "new alias must not revive an expired source local");
        var branchLocal=method("class F {static void f(boolean c){int[] a=new int[2];if(c){int[] b=a;}else{int[] d=a;}}}");
        check(branchLocal.complete(),"branch-local aliases admitted: "+branchLocal.unresolved());
        check(branchLocal.snapshots().getLast().mustLocal()[1]==1,
                "branch-local aliases must expire before join");
        var code=Map.of("F.java","class F {static void f(){int[] a=new int[2];}}");
        var refs=ReferenceFlow.analyze(code,null);
        var goodFlow=SourceLiveness.analyzeExtended(code,null);
        var forced=new java.util.ArrayList<>(goodFlow.methods());
        int selected=-1;
        for(int i=0;i<forced.size();i++)
            if(forced.get(i).signature().contains("#f(")){selected=i;break;}
        check(selected>=0,"original method in CFG");
        var original=forced.get(selected);
        forced.set(selected,new SourceLiveness.MethodFlow(
                original.source(),original.signature(),original.position(),List.of(),List.of(),
                -1,-1,-1,java.util.Optional.empty(),List.of("forced unresolved CFG")));
        var incomplete=new SourceLiveness.Result(goodFlow.facts(),goodFlow.limits(),
                goodFlow.proofKey(),forced);
        var refused=CRefPathCertainty.analyze(code,refs,incomplete,null)
                .methods().stream().filter(m->m.signature().contains("#f("))
                .findFirst().orElseThrow();
        check(!refused.complete() && refused.snapshots().isEmpty(),
                "incomplete compiler CFG may not produce positive MAY/MUST evidence");
        // Compile every two-way local assignment combination, in both
        // branch orders. MAY must union possible origins, MUST intersect
        // only references guaranteed at each control-flow successor.
        String[] expressions={"a","null","new int[3]"};
        for(int a=0;a<3;a++)for(int b=0;b<3;b++)for(int order=0;order<2;order++){
            String left=expressions[order==0?a:b],right=expressions[order==0?b:a];
            String fixture="class F {static void f(boolean c){int[] a=new int[2];int[] b=null;"
                    +"if(c){b="+left+";}else{b="+right+";}}}";
            var mixed=method(fixture);
            check(mixed.complete(),"javac branch join accepted: "+mixed.unresolved());
            var joined=mixed.snapshots().getLast();
            int expectedMay=1+((a==0||b==0)?1:0);
            int expectedMust=1+((a==0&&b==0)?1:0);
            check(joined.mayLocal()[1]==expectedMay && joined.mustLocal()[1]==expectedMust,
                    "MAY union and MUST intersection at branch join");
            check(!joined.authorizesReclamation(),"compiler branch facts cannot free objects");
        }
        // Compiler-attributed conditional reference expressions: a ternary
        // is one source-local binding, not two simultaneous heap references.
        String[] arms={"a","null","new int[3]"};
        for(int a=0;a<arms.length;a++)for(int b=0;b<arms.length;b++)
                for(int reversed=0;reversed<2;reversed++){
            String left=arms[reversed==0?a:b];
            String right=arms[reversed==0?b:a];
            String snippet="class F {static void f(boolean c){int[] a=new int[2];"
                    +"int[] b=c ? "+left+" : "+right+";}}";
            var ternary=method(snippet);
            check(ternary.complete(),"javac conditional expressions admitted: "+ternary.unresolved());
            var state=ternary.snapshots().getLast();
            int expectedMay=1+((a==0||b==0)?1:0);
            int expectedMust=1+((a==0&&b==0)?1:0);
            check(state.mayLocal()[1]==expectedMay && state.mustLocal()[1]==expectedMust,
                    "conditional MAY union and MUST intersection");
            check(!state.authorizesReclamation(),"conditional local facts cannot free objects");
        }
        var externalTernary=method("class F {static void f(boolean c,int[] outside){"
                +"int[] a=new int[2];int[] b=c ? outside : a;}}");
        check(externalTernary.complete(),"external conditional attributed");
        var externalState=externalTernary.snapshots().getLast();
        check(externalState.mayLocal()[0]==2 && externalState.mustLocal()[0]==0,
                "external parameter remains TOP, never definite ownership");
        check(!externalState.singleDefiniteLocalCandidate(1),
                "unknown parameter blocks uniqueness candidate");
        var nested=method("class F {static void f(boolean c){int[] a=new int[2];"
                +"int[] b=c ? (c ? a : null) : a;}}");
        check(nested.complete(),"nested conditional attributed");
        check(nested.snapshots().getLast().mustLocal()[1]==1
                && nested.snapshots().getLast().mayLocal()[1]==2,
                "nested MAY/MUST joins preserve conditional alternatives");
        var booleanCondition=method("class F {static void f(boolean c){int[] a=new int[2];"
                +"if(!c || false){a=null;}}}");
        check(booleanCondition.complete(),"effect-free boolean condition attributed");
        check(booleanCondition.snapshots().getLast().mustLocal()[1]==0,
                "pure boolean predicates preserve conservative join");
        var forbiddenField=method("class F {static boolean field;"
                +"static void f(){int[] a=new int[2];int[] b=field ? a : null;}}");
        check(!forbiddenField.complete() && forbiddenField.snapshots().isEmpty(),
                "mutable field predicate cannot certify reference ownership");
        var forbiddenCall=method("class F {static boolean call(){return true;}"
                +"static void f(){int[] a=new int[2];int[] b=call() ? a : null;}}");
        check(!forbiddenCall.complete() && forbiddenCall.snapshots().isEmpty(),
                "method call predicate cannot certify reference ownership");
        var forbiddenIf=method("class F {static boolean field;"
                +"static void f(){int[] a=new int[2];if(field){a=null;}}}");
        check(!forbiddenIf.complete(),"field-read conditional refuses path certificate");
        String deep="a";
        for(int i=0;i<12;i++) deep="(c ? "+deep+" : null)";
        var bounded=method("class F {static void f(boolean c){int[] a=new int[2];"
                +"int[] b="+deep+";}}");
        check(!bounded.complete(),"nested ternary exceeds conservative expression budget");
        // Complete Java21 primitive array component taxonomy. The source
        // reference type is still an ARRAY; only its component is primitive.
        String[] primitives={"boolean","byte","short","char",
                "int","long","float","double"};
        for(String element:primitives){
            String fixture="class F {static void f(boolean c){"+element
                    +"[] a=new "+element+"[2];"+element+"[] b=c ? a : null;}}";
            var primitive=method(fixture);
            check(primitive.complete(),"all Java primitive arrays admitted: "+element
                    +" "+primitive.unresolved());
            var counts=primitive.snapshots().getLast();
            check(counts.mayLocal()[1]==2 && counts.mustLocal()[1]==1,
                    "conditional source-local count on primitive "+element);
            check(!counts.authorizesReclamation(),"primitive array cannot be reclaimed by source facts");
        }
        var nestedArray=method("class F {static void f(){int[][] a=new int[2][3];}}");
        check(!nestedArray.complete() && nestedArray.snapshots().isEmpty(),
                "array of reference-array components needs a separate alias model");
        var objectArray=method("class F {static void f(){String[] a=new String[2];}}");
        check(!objectArray.complete() && objectArray.snapshots().isEmpty(),
                "object-array alias semantics are outside restricted primitive model");
        // Source-time truth lattice for compiler-proven, side-effect-free
        // Boolean predicates. Unknown parameters remain path-joined.
        String[] predicates={"true","false","c","!c","!false","!true",
                "c && false","false && c","c || true","true || c",
                "c && true","c || false","(c || true) && !false",
                "(c && false) || !true","!(c && false)","!(c || true)",
                "!(!(!c))","true && false","false || true"};
        int[] truth={1,-1,0,0,1,-1,-1,-1,1,1,0,0,1,-1,1,-1,0,-1,1};
        for(int i=0;i<predicates.length;i++){
            String expr=predicates[i];
            var ternary=method("class F {static void f(boolean c){int[] a=new int[2];"
                    +"int[] b=("+expr+") ? a : null;}}");
            check(ternary.complete(),"compile-time Boolean ternary qualified: "+expr
                    +" "+ternary.unresolved());
            var data=ternary.snapshots().getLast();
            check(data.mayLocal()[1]==(truth[i]==-1 ? 1 : 2),
                    "predicate-specific MAY local aliases");
            check(data.mustLocal()[1]==(truth[i]==1 ? 2 : 1),
                    "predicate-specific definite local aliases");
            var ifBranch=method("class F {static void f(boolean c){int[] a=new int[2];"
                    +"if("+expr+"){a=null;}}}");
            check(ifBranch.complete(),"compiled pure Boolean IF qualified: "+expr
                    +" "+ifBranch.unresolved());
            var joined=ifBranch.snapshots().getLast();
            check(joined.mayLocal()[1]==(truth[i]==1 ? 0 : 1),
                    "IF feasible-path MAY reference count");
            check(joined.mustLocal()[1]==(truth[i]==-1 ? 1 : 0),
                    "IF feasible-path MUST reference count");
            check(!data.authorizesReclamation() && !joined.authorizesReclamation(),
                    "Boolean compiler hints cannot release JVM objects");
        }
        // Even a definitely dead source branch is analyzed before selection:
        // do not use static precompute to conceal calls or heap effects.
        var deadCall=method("class F {static void f(){int[] a=new int[2];"
                +"if(false){System.out.println(a);}}}");
        check(!deadCall.complete() && deadCall.snapshots().isEmpty(),
                "unreachable but unmodeled effects must still refuse");
        var deadExpr=method("class F {static int[] call(){return null;}"
                +"static void f(){int[] a=new int[2];int[] b=true ? a : call();}}");
        check(!deadExpr.complete() && deadExpr.snapshots().isEmpty(),
                "unselected conditional arm may not hide unsupported call");
        System.out.println("M3_CREF_PATH_CERTAINTY_PASS checks="+checks);
    }
}
