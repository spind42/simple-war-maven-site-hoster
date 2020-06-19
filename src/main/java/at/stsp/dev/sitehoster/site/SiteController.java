package at.stsp.dev.sitehoster.site;

import at.stsp.dev.sitehoster.site.configuration.GitConfig;
import at.stsp.dev.sitehoster.site.configuration.SiteConfig;
import at.stsp.dev.sitehoster.site.configuration.SiteHostAppConfigurationProperties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class SiteController {

    private static final Logger LOGGER = LogManager.getLogger(SiteController.class);

    @Autowired
    private SiteHostAppConfigurationProperties config;

    private Path tempDirPath;

    @PostConstruct
    public void init() {
        String tempDir = config.getTempDir();
        if (tempDir.startsWith("file://")) {
            tempDir = tempDir.replaceFirst("file://", "");
        }
        if (tempDir.startsWith("file:")) {
            tempDir = tempDir.replaceFirst("file:", "");
        }

        tempDirPath = Paths.get(tempDir);
        if (!Files.exists(tempDirPath)) {
            LOGGER.info("Directory [{}] (tempDir) does not exist, creating it...", tempDirPath);
            try {
                Files.createDirectories(tempDirPath);
            } catch (IOException e) {
                LOGGER.error("Failed to create temp directory [{}]", tempDirPath);
            }
        }

        updateSites();

    }

    private void updateSites() {
        config.getSites().forEach(this::updateSite);
    }

    private void updateSite(SiteConfig siteConfig) {
        String name = siteConfig.getName();
        tempDirPath.resolve(name);

        GitConfig gitSource = siteConfig.getGitSource();
        if (gitSource != null) {
            updateGitSite(siteConfig);
        }
    }

    private void updateGitSite(SiteConfig siteConfig) {
        try {
            String name = siteConfig.getName();
            Path resolve = tempDirPath.resolve(name);

            GitConfig repoConfig = siteConfig.getGitSource();

//        Git.lsRemoteRepository()
//                .setRemote(repoConfig.getUri())
//

            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();

            Repository repo = repositoryBuilder.setGitDir(resolve.resolve(".git").toFile())
                    .readEnvironment()
                    .findGitDir()
                    .setMustExist(false)
                    .build();
            repo.




//            Git.cloneRepository()
//                    .setURI(siteConfig.getGitSource().getUri())
//                    .setDirectory(resolve.toFile())
//                    .setMirror(true)
//
//                    .call();

        } catch (Exception  e) {
            LOGGER.error("An error has occured...", e);
        }


    }


}
