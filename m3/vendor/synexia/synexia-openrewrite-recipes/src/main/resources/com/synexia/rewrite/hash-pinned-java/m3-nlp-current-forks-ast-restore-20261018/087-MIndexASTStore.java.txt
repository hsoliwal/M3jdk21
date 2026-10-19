// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import com.synexia.mindex.experiment.MIndexString;

/** Internal stable coordinate domain; implementations never rebind an existing ID. */
abstract class MIndexASTStore {
    abstract MIndexASTNode node(int id);
    abstract MIndexString symbol(int id);
    public abstract int size();
}
