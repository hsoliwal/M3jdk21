// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.mindex.ast;

import jdk.internal.m3.mindex.experiment.MIndexString;

/** Internal stable coordinate domain; implementations never rebind an existing ID. */
abstract class MIndexASTStore {
    abstract MIndexASTNode node(int id);
    abstract MIndexString symbol(int id);
    public abstract int size();
}
