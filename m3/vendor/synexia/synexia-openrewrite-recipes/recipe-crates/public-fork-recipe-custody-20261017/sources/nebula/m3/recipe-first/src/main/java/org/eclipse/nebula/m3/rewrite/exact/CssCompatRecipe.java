// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;
import java.util.List;
import org.openrewrite.Recipe;
/** Sealed reuse of the existing Nebula exact-Java compatibility owner. */
public final class CssCompatRecipe extends Recipe {
    private record Target(String path,String before,String after,String resource) {}
    private static final List<Target> TARGETS=List.of(
        new Target("widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css/src/org/eclipse/nebula/widgets/oscilloscope/css/OscilloscopePropertyHandler.java","18f053da8706ae6ac3e537289c66e085e251a4692b0ceccffb27bfde94f08661","9b40e59a3894865dc03f37d9f4820f430ae0689720dcaf9300a4f94010885b50","/org/eclipse/nebula/m3/rewrite/exact/css-compat/OscilloscopePropertyHandler.java.txt"),
        new Target("widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css/src/org/eclipse/nebula/widgets/roundedswitch/css/RoundedSwitchPropertyHandler.java","0dd1f91b486c559e3474c4bbd9eb8ef6eaa6b76bee5bd7d15bd9c0aab37dccc3","0fc7f30752819820ed3a2ed036dbed399a75cf5b3829dff187baf63af0b1fe3c","/org/eclipse/nebula/m3/rewrite/exact/css-compat/RoundedSwitchPropertyHandler.java.txt"),
        new Target("widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css/src/org/eclipse/nebula/widgets/tablecombo/css/TableComboPropertyHandler.java","e887f18a34a2fd9c607467c38921d0b8eaaeaa3dfcddbaa551bd895fbeb9ec89","4b961a1aaf44f4df2ca2b5fc2883f1d7b5a3bfb443cd9510f9e1902da9ceebe5","/org/eclipse/nebula/m3/rewrite/exact/css-compat/TableComboPropertyHandler.java.txt"));
    @Override public String getDisplayName(){return "Complete Nebula CSS value compatibility";}
    @Override public String getDescription(){return "Reuses the sealed CDateTime/Grid value migration for Oscilloscope, RoundedSwitch and TableCombo while retaining legacy classification and numeric access.";}
    @Override public List<Recipe> getRecipeList(){
        return TARGETS.stream().<Recipe>map(target -> new NebulaM3ExactJavaSnapshotRecipe() {
            @Override public String getDisplayName(){return "Migrate "+target.path();}
            @Override public String getDescription(){return CssCompatRecipe.this.getDescription();}
            @Override protected String repositoryPath(){return target.path();}
            @Override protected String moduleRelativePath(){return target.path().substring(target.path().indexOf("/src/")+1);}
            @Override protected String beforeSha256(){return target.before();}
            @Override protected String afterSha256(){return target.after();}
            @Override protected String afterResource(){return target.resource();}
        }).toList();
    }
}
