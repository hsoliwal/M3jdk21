/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/** Named packaging of the existing, exactly restored M3 compiler/source/AST closure. */
module jdk.m3.compiletime {
    requires transitive jdk.compiler;
    exports jdk.m3.compiletime.ast;
    exports jdk.internal.m3.mindex;
    exports jdk.internal.m3.mindex.ast;
    exports jdk.internal.m3.mindex.ast.partial;
    exports jdk.internal.m3.mindex.compiler.ast;
    exports jdk.internal.m3.mindex.experiment;
    exports jdk.internal.m3.mindex.swap;
    exports jdk.internal.m3.nativeinterop;
    uses jdk.internal.m3.mindex.MIndexTensorBackend;
    provides com.sun.source.util.Plugin with jdk.m3.compiletime.ast.M3ASTPlugin;
}
