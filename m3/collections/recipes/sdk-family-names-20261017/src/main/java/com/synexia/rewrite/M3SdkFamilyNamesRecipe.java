/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
/** Existing source-seal recipe with a named, reviewed five-family SDK template set. */
public final class M3SdkFamilyNamesRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() { return "/com/synexia/rewrite/sdk-family-names-20261017/"; }
    @Override public String getDisplayName() { return "Name M3 SDK families and compose Apache and Guava views"; }
    @Override public String getDescription() {
        return "Adds exact API-family and M3-owner identities with existing donor contract adapters from sealed templates.";
    }
}
