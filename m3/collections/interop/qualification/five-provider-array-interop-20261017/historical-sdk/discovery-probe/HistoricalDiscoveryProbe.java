import io.github.classgraph.ClassGraph;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import java.util.Properties;
public class HistoricalDiscoveryProbe {
 public static void main(String[] arguments) throws Exception {
  var loader=HistoricalDiscoveryProbe.class.getClassLoader();
  System.out.println("loader="+loader);
  System.out.println("resource="+loader.getResource("META-INF/rewrite/m3-sdk-family-names.yml"));
  try (var scan=new ClassGraph().ignoreParentClassLoaders().overrideClassLoaders(loader).acceptPaths("META-INF/rewrite").verbose().scan()) {
   System.out.println("classpath="+scan.getClasspath());
   System.out.println("resources="+scan.getAllResources().getPaths());
  }
  var url=loader.getResource("META-INF/rewrite/m3-sdk-family-names.yml");
  try (var input=url.openStream()) {
   var yaml=new YamlResourceLoader(input,url.toURI(),new Properties(),loader);
   System.out.println("direct recipes="+yaml.listRecipes().stream().map(x->x.getName()).toList());
  }
  var env=Environment.builder().scanClassLoader(loader).build();
  System.out.println("environment recipes="+env.listRecipes().stream().map(x->x.getName()).toList());
 }
}
