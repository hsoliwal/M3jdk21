/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.parser.test;

import java.net.URI;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

/** Executed inside a real Equinox bundle, with a real workspace and unsaved working copy. */
public final class M3JdtWorkspaceProbe {
    private M3JdtWorkspaceProbe() { }
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    private static M3ASTIndex snapshot(ICompilationUnit copy, WorkingCopyOwner owner) throws Exception {
        CompilationUnit root = copy.reconcile(AST.getJLSLatest(), true, owner, null);
        check(root != null, "real reconcile AST");
        M3File source = M3File.of(URI.create("workspace:/sample/src/Sample.java"), copy.getBuffer().getContents());
        return M3ASTIngress.eclipseJdtResolved(source, root, M3ASTIngress.Limits.defaults());
    }
    private static boolean bound(M3ASTIndex index, String name) {
        for (int row = 0; row < index.nodeCount(); row++) {
            if (index.kind(row).equals("JDT_SimpleName") && index.hasExactRange(row)
                    && index.sourceText(row).equals(name) && index.bindingKey(row) != null) return true;
        }
        return false;
    }
    public static String run() throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("sample");
        project.create(null); project.open(null);
        var description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID }); project.setDescription(description, null);
        var sourceFolder = project.getFolder("src"); sourceFolder.create(true, true, null);
        var javaProject = JavaCore.create(project);
        javaProject.setOption(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_21);
        javaProject.setOption(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_21);
        javaProject.setRawClasspath(new org.eclipse.jdt.core.IClasspathEntry[] {
            JavaCore.newSourceEntry(sourceFolder.getFullPath()) }, project.getFullPath().append("bin"), null);
        var fragment = javaProject.getPackageFragmentRoot(sourceFolder).createPackageFragment("", true, null);
        String originalText = "class Sample { int count; int read() { return count; } }";
        ICompilationUnit original = fragment.createCompilationUnit("Sample.java", originalText, true, null);
        WorkingCopyOwner owner = new WorkingCopyOwner() { };
        ICompilationUnit copy = original.getWorkingCopy(owner, null);
        try {
            M3ASTIndex first = snapshot(copy, owner);
            check(bound(first, "count"), "working-copy local binding");
            String edited = originalText.replace("count", "total");
            copy.getBuffer().setContents(edited);
            M3ASTIndex second = snapshot(copy, owner);
            check(bound(second, "total"), "unsaved edit bindings");
            check(!bound(second, "count"), "no stale binding retained");
            check(first.source().text().equals(originalText), "first snapshot immutable");
            check(second.source().text().equals(edited), "unsaved source selected");
            check(original.getSource().equals(originalText), "original disk unit unchanged");
            check(!first.source().utf16Sha256().equals(second.source().utf16Sha256()), "source generation changes");
            copy.getBuffer().setContents("class Sample { int read() { return missing; } }");
            M3ASTIndex broken = snapshot(copy, owner);
            check(broken.problemCount() > 0, "reconcile diagnostics retained");
            copy.getBuffer().setContents(originalText);
            M3ASTIndex restored = snapshot(copy, owner);
            check(restored.source().utf16Sha256().equals(first.source().utf16Sha256()), "restored source seal");
            check(bound(restored, "count"), "restored bindings");
            return "M3_JDT_WORKSPACE_PASS checks=" + checks;
        } finally {
            copy.discardWorkingCopy(); project.delete(true, true, null);
        }
    }
}
