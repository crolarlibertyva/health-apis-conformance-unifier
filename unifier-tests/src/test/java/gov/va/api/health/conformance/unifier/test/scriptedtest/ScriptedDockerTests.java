package gov.va.api.health.conformance.unifier.test.scriptedtest;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ScriptedTest.class)
public class ScriptedDockerTests {

  private static final List<String> PUBLISH_GOALS = Arrays.asList("clean", "compile");

  //  private Invoker invoker;

  //  private File localRepositoryDir = new File(System.getProperty("m2.repo"));

  //  @Before
  //  public void init() {
  //    Invoker newInvoker = new DefaultInvoker();
  //    newInvoker.setLocalRepositoryDirectory(localRepositoryDir);
  //
  //    this.invoker = newInvoker;
  //  }

  // this method will be called repeatedly, and fire off new builds...
  //  @Test
  //  @Ignore
  //  public void publishSite(File siteDirectory) throws MavenInvocationException {
  //    InvocationRequest request = new DefaultInvocationRequest();
  //    request.setBaseDirectory(Paths.get("./target/").toFile());
  //    request.setGoals(PUBLISH_GOALS);
  //
  //    InvocationResult result = invoker.execute(request);
  //    System.out.println(result.toString());
  //  }

  @Test
  @Category(ScriptedTest.class)
  public void showTestCategore() {

    System.out.println("============================> Scripted Tests");
  }
}
