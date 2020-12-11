package at.stsp.dev.sitehoster.site.git;

import at.stsp.dev.sitehoster.SiteHostApp;
import at.stsp.dev.sitehoster.site.configuration.GitConfig;
import at.stsp.dev.sitehoster.site.configuration.SiteConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;


import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({SpringExtension.class})
@SpringBootTest(classes = SiteHostApp.class)
@Testcontainers
class GitSiteResourceResolverITCase {

    @Autowired
    GitSiteResourceResolver gitSiteResourceResolver;

    @Container
    private static final GenericContainer SSH_KEY_GIT_CONTAINER = new GenericContainer("spind42/test-git-repo:latest")
//    private static final GenericContainer SSH_KEY_GIT_CONTAINER = new GenericContainer("jkarlos/git-server-docker")

            .withCopyFileToContainer(MountableFile.forHostPath("~/.ssh/id_rsa.pub"), "/git-server/keys/id_rsa.pub")

//        .withFileSystemBind("src/test/resources/ssh/", "/git-server/keys/", BindMode.READ_ONLY)
            .withCopyFileToContainer(MountableFile.forClasspathResource("/ssh/ssh-key1.pub"), "/git-server/keys/ssh-key1.pub")
//            .withCopyFileToContainer(MountableFile.forHostPath(Paths.get("~/.ssh/gitkey_rsa.pub")), "/git-server/keys/gitkey.pub")
        .withCommand("/bin/sh /git-server/start.sh");


    @Test
    public void testSshKeyAccess() {
        assertThat(SSH_KEY_GIT_CONTAINER.isRunning()).isTrue();

        SiteConfig siteConfig = new SiteConfig();
        siteConfig.setName("st1");
        GitConfig gitConfig = new GitConfig();
        gitConfig.setPrivateKeyFile(new ClassPathResource("/ssh/ssh-key1"));
        Integer sshPort = SSH_KEY_GIT_CONTAINER.getMappedPort(22);
        String dockerHost = SSH_KEY_GIT_CONTAINER.getHost();
        String gitUri = String.format("ssh://git@%s:%s/git-server/repos/test1.git", dockerHost, sshPort );
        gitConfig.setUri(gitUri);
        siteConfig.setGitSource(gitConfig);

        System.out.println("GIT URI: "+ gitUri);

        gitSiteResourceResolver.updateGitSite(siteConfig);

    }

//    @Test
//    public void sleep() throws InterruptedException {
//        Thread.sleep(350000);
//    }

}