package gov.va.api.health.conformance.unifier.test.scriptedtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ScriptedTest.class)
@Slf4j
public class ScriptedDockerTests {
  private static File localRepositoryDir;

  private static DockerClient dockerClient;

  private Invoker invoker;

  private ObjectMapper mapper = new ObjectMapper();

  @BeforeClass
  public static void initDockerClient() {
    String repoPath = System.getProperty("user.home") + "/.m2/repository";
    String m2Repo = System.getProperty("m2.repo");
    repoPath = m2Repo != null && !m2Repo.isEmpty() ? m2Repo : repoPath;
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
    ObjectNode expectedFileData =
        (ObjectNode)
            mapper.readTree(
                Paths.get("src/test/resources/" + resultType + "/fileData.json").toFile());
    expectedFileData.remove("date");
    final Path resultsPathRoot = Paths.get("./target/s3results/tmp");
    final Path bucketRoot =
        Files.list(resultsPathRoot)
            .filter(f -> f.getFileName().toString().startsWith("s3mockFileStore"))
            .findAny()
            .map(s3 -> s3.resolve("testbucket"))
            .get();
    Path resultsPath = bucketRoot.resolve(resultType);
    ObjectNode fileData =
        (ObjectNode) mapper.readTree(resultsPath.resolve("fileData").toAbsolutePath().toFile());
    fileData.remove("date");
    Assert.assertEquals(expectedFileData, fileData);
  }

  @After
  public void containerDown() throws MavenInvocationException {
    mavenGoals(Arrays.asList("-Plocaltest docker:stop"));
  }

  @Before
  public void initInvoker() throws MavenInvocationException {
    log.info("m2.repo=" + localRepositoryDir.getAbsolutePath());
    Invoker newInvoker = new DefaultInvoker();
    newInvoker.setLocalRepositoryDirectory(localRepositoryDir);
    this.invoker = newInvoker;
    mavenGoals(Arrays.asList("-Plocaltest docker:start"));
  }

  private void mavenGoals(final List<String> mvnGoals) throws MavenInvocationException {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setBaseDirectory(Paths.get("..").toFile());
    request.setGoals(mvnGoals);
    InvocationResult result = invoker.execute(request);
    log.info(result.toString());
  }

  @Test
  public void mavenScripts() throws MavenInvocationException, IOException {
    log.info("============================> Scripted Tests <============================");
    mavenGoals(
        Arrays.asList(
            "-Plocaltest -pl conformance-unifier -am spring-boot:run -Dspring-boot.run.arguments=\"dstu2,metadata,https://api.va.gov/services/fhir/v0/dstu2/metadata\""));
    untar(
        dockerClient.copyArchiveFromContainerCmd("conformanceS3Mock", "/tmp/").exec(),
        Paths.get("./target/s3results").toFile());
    checkS3Results("dstu2-metadata");
  }
}
