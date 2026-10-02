/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import javax.lang.model.element.Modifier;
import java.util.*;
public class M3StringReads {
 public static void main(String[] a)throws Exception {
  JavaCompiler c=ToolProvider.getSystemJavaCompiler(); var fm=c.getStandardFileManager(null,null,null);
  JavacTask task=(JavacTask)c.getTask(null,fm,null,List.of("-proc:none"),null,fm.getJavaFileObjects(a[0]));
  CompilationUnitTree u=task.parse().iterator().next(); var p=Trees.instance(task).getSourcePositions();
  new TreePathScanner<Void,Void>() {
   MethodTree method;
   public Void visitMethod(MethodTree t,Void v){var old=method;method=t;super.visitMethod(t,v);method=old;return null;}
   void emit(Tree t){System.out.println(p.getStartPosition(u,t)+" "+p.getEndPosition(u,t)+" "+method.getName()+" "+t);}
   public Void visitIdentifier(IdentifierTree t,Void v){
    if(!(getCurrentPath().getParentPath().getLeaf() instanceof MethodInvocationTree call && call.getMethodSelect() == t) && t.getName().contentEquals("value") && method!=null && !method.getName().contentEquals("<init>") && !method.getName().contentEquals("value") && !method.getModifiers().getFlags().contains(Modifier.STATIC)) emit(t);
    return super.visitIdentifier(t,v);
   }
   public Void visitMemberSelect(MemberSelectTree t,Void v){
    if(!(getCurrentPath().getParentPath().getLeaf() instanceof MethodInvocationTree call && call.getMethodSelect() == t) && t.getIdentifier().contentEquals("value") && method!=null && !method.getName().contentEquals("<init>")) emit(t);
    return super.visitMemberSelect(t,v);
   }
  }.scan(u,null);
 }
}
