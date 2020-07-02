package gov.va.api.health.conformance.unifier.test.scriptedtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ScriptedTest.class)
public class ScriptedDockerTests {
  private static File localRepositoryDir;

  private static DockerClient dockerClient;

  private Invoker invoker;

  private ObjectMapper mapper = new ObjectMapper();

  @BeforeClass
  public static void initialize() {
    String repoPath = System.getProperty("user.home") + "/.m2/repository";
    localRepositoryDir = Paths.get(repoPath).toFile();
    dockerClient = DockerClientBuilder.getInstance().build();
  }

  public static void untar(InputStream tarIn, File out) throws IOException {
    try (TarArchiveInputStream fin = new TarArchiveInputStream(tarIn)) {
      TarArchiveEntry entry;
      while ((entry = fin.getNextTarEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        File curfile = new File(out, entry.getName());
        File parent = curfile.getParentFile();
        if (!parent.exists()) {
          parent.mkdirs();
        }
        IOUtils.copy(fin, new FileOutputStream(curfile));
      }
    }
  }

  private void checkS3Results(String resultType) throws IOException {
    Object expectedFileData =
        mapper.readValue(
            this.getClass().getResourceAsStream(resultType + "/fileData.json"), HashMap.class);
    final Path resultsPath =
        Paths.get("./target/s3results/s3mockFileStore1593641003848/testbucket" + resultType);
    Object fileData =
        mapper.readValue(resultsPath.resolve("fileData").toAbsolutePath().toFile(), HashMap.class);
    Assert.assertEquals(expectedFileData, fileData);
  }

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

  @Test
  public void mavenScripts() throws MavenInvocationException, IOException {
    mavenGoals(Arrays.asList("-Plocaltest docker:start"));
    mavenGoals(
        Arrays.asList(
            "-Plocaltest -pl conformance-unifier -am spring-boot:run -Dspring-boot.run.arguments=\"dstu2,metadata,https://api.va.gov/services/fhir/v0/dstu2/metadata\""));
    untar(
        dockerClient
            .copyArchiveFromContainerCmd("conformanceS3Mock", "/tmp/s3mockFileStore1593641003848/")
            .exec(),
        Paths.get("./target/s3results").toFile());
    checkS3Results("dstu-metadata");
    // mavenGoals(Arrays.asList("-Plocaltest docker:stop"));
  }

  @Test
  public void showTestCategory() {
    System.out.println("============================> Scripted Tests");
    // System.out.println(System.getProperties().toString());
  }
}
