package gov.va.api.health.conformance.unifier.test.scriptedtest;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
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

  private Invoker invoker;

  private File localRepositoryDir = new File(System.getProperty("m2.repo"));
  private DockerClient dockerClient = DockerClientBuilder.getInstance().build();

  @Before
  public void init() {
    System.out.println("m2.repo=" + localRepositoryDir.getAbsolutePath());
    Invoker newInvoker = new DefaultInvoker();
    newInvoker.setLocalRepositoryDirectory(localRepositoryDir);
    //
    this.invoker = newInvoker;
  }

  private void mavenGoals(final List<String> mvnGoals) throws MavenInvocationException {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setBaseDirectory(Paths.get("..").toFile());
    request.setGoals(mvnGoals);
    InvocationResult result = invoker.execute(request);
    System.out.println(result.toString());
  }

  // this method will be called repeatedly, and fire off new builds...
  @Test
  public void mavenGoals() throws MavenInvocationException {
    mavenGoals(Arrays.asList("-Plocaltest docker:start"));
    mavenGoals(
        Arrays.asList(
            "-Plocaltest -pl conformance-unifier -am spring-boot:run -Dspring-boot.run.arguments=\"dstu2,metadata,https://api.va.gov/services/fhir/v0/dstu2/metadata\""));
    dockerClient.copyArchiveFromContainerCmd(
        "/tmp/s3mockFileStore1593641003848/", "target/s3results");
    //    mavenGoals(Arrays.asList("-Plocaltest docker:stop"));
  }

  @Test
  public void showTestCategory() {
    System.out.println("============================> Scripted Tests");
  }
}
