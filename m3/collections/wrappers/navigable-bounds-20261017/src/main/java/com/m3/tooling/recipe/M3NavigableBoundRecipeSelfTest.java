// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.recipe;

/** Mutation-refusing recipe contract independent of the repository checkout. */
public final class M3NavigableBoundRecipeSelfTest {
    private M3NavigableBoundRecipeSelfTest() {}

    public static void main(String[] args) {
        String source = "package com.m3.collections;\npublic final class M3FilteredNavigableSet<E> {\n"
                + M3NavigableBoundRecipe.BEFORE + "\n}\n";
        String once = M3NavigableBoundRecipe.transform(source);
        if (source.equals(once) || !once.contains("backing.lower(probe)")) {
            throw new AssertionError("recipe did not transform the bounded methods");
        }
        if (!once.equals(M3NavigableBoundRecipe.transform(once))) {
            throw new AssertionError("not idempotent");
        }
        boolean refused = false;
        try {
            M3NavigableBoundRecipe.transform(
                    source.replace("headSet(e, false)", "headSet(e, true)"));
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        if (!refused) throw new AssertionError("changed preimage admitted");
        System.out.println("M3_NAV_BOUND_RECIPE_FIXED_POINT_AND_REFUSAL_PASS");
    }
}
