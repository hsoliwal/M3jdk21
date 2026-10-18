/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/** Named packaging of the existing, exactly restored M3 compiler/source/AST closure. */
module jdk.m3.compiletime {
    requires transitive jdk.compiler;
    exports com.m3.compiletime.ast;
    exports com.synexia.mindex;
    exports com.synexia.mindex.ast;
    exports com.synexia.mindex.ast.partial;
    exports com.synexia.mindex.compiler.ast;
    exports com.synexia.mindex.experiment;
    exports com.synexia.mindex.swap;
    exports com.synexia.nativeinterop;
    uses com.synexia.mindex.MIndexTensorBackend;
    provides com.sun.source.util.Plugin with com.m3.compiletime.ast.M3ASTPlugin;
}
