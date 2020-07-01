package gov.va.api.health.conformance.unifier.test.scriptedtest;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ScriptedTest.class)
public class ScriptedDockerTests {

  private static final List<String> TEST_GOALS = Arrays.asList("clean", "test");

  private Invoker invoker;

  private File localRepositoryDir = new File(System.getProperty("m2.repo"));

  @Before
  public void init() {
    System.out.println("m2.repo=" + localRepositoryDir.getAbsolutePath());
    Invoker newInvoker = new DefaultInvoker();
    newInvoker.setLocalRepositoryDirectory(localRepositoryDir);
    //
    this.invoker = newInvoker;
  }

  // this method will be called repeatedly, and fire off new builds...
  @Test
  public void publishSite() throws MavenInvocationException {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setBaseDirectory(Paths.get("./target/").toFile());
    request.setGoals(TEST_GOALS);
    //
    InvocationResult result = invoker.execute(request);
    System.out.println(result.toString());
  }

  @Test
  @Category(ScriptedTest.class)
  public void showTestCategore() {

    System.out.println("============================> Scripted Tests");
  }
}
