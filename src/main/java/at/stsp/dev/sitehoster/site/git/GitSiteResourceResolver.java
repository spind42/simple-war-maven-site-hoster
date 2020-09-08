package at.stsp.dev.sitehoster.site.git;

import at.stsp.dev.sitehoster.site.ResourceQuery;
import at.stsp.dev.sitehoster.site.SiteResourceResolver;
import at.stsp.dev.sitehoster.site.SiteResourceResolverRegistration;
import at.stsp.dev.sitehoster.site.SiteResourceResolverRegistrationProvider;
import at.stsp.dev.sitehoster.site.configuration.GitConfig;
import at.stsp.dev.sitehoster.site.configuration.SiteConfig;
import at.stsp.dev.sitehoster.site.configuration.SiteHostAppConfigurationProperties;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
@Log4j2
public class GitSiteResourceResolver implements SiteResourceResolver, SiteResourceResolverRegistrationProvider {

//    private static final Logger log = LogManager.getLogger(GitSiteResourceResolver.class);

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
            log.info("Directory [{}] (tempDir) does not exist, creating it...", tempDirPath);
            try {
                Files.createDirectories(tempDirPath);
            } catch (IOException e) {
                log.error("Failed to create temp directory [{}]", tempDirPath);
            }
        }

        updateSites();

    }

    public void copyIntoOuputStream(@NotNull SiteConfig siteConfig, @NotNull ResourceQuery query, @NotNull OutputStream os) {
        try {
            String path = query.getResourcePath();
            if (StringUtils.isEmpty(path)) {
                throw new IllegalArgumentException("Query Path is not allowed to be null!");
            }
            Git git = getRepositoryFromConfig(siteConfig);
            GitConfig gitSource = siteConfig.getGitSource();
            String ref = gitSource.getDefaultRef();
            Repository repository = git.getRepository();

            ObjectId lastCommitId = repository.resolve(ref);
            if (lastCommitId == null) {
                throw new RuntimeException(String.format("Was not able to resolve ref [%s]", ref));
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                RevTree tree = commit.getTree();


                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(path));
                    if (!treeWalk.next()) {
                        throw new IllegalStateException(String.format("Did not find resource [%s] within git repo", path));
                    }
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);

                    // and then one can the loader to read the file
                    loader.copyTo(os);

                }
                revWalk.dispose();
            }

//            return new ByteArrayInputStream("Hallo Welt".getBytes());

        } catch (IOException e) {
            throw new RuntimeException(String.format("Error while opening resource [%s] from git repository", query.getResourcePath()), e);
        }
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

    Git getRepositoryFromConfig(SiteConfig siteConfig) {
        try {
            String name = siteConfig.getName();
            Path repoHostDir = tempDirPath.resolve(name);
            GitConfig gitSource = siteConfig.getGitSource();

            Path gitRepoConfigDir = repoHostDir.resolve(".git");

            Git open = Git.open(gitRepoConfigDir.toFile());
            log.debug("Successfully opened git from siteConfig [{}]", siteConfig);
            return open;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error while get RepositoryFromSiteConfig [%s]", siteConfig), e);
        }
    }

    protected void updateGitSite(SiteConfig siteConfig) {
        try {
            String name = siteConfig.getName();
            Path repoHostDir = tempDirPath.resolve(name);
            GitConfig gitSource = siteConfig.getGitSource();

            Path gitRepoConfigDir = repoHostDir.resolve(".git");
            if (!Files.exists(gitRepoConfigDir)) {
                log.debug("Repo under [{}] does not exist - cloning branches [{}] from [{}]", gitRepoConfigDir, gitSource.getBranches(), gitSource.getUri());
                CloneCommand cloneCommandBuilder = Git.cloneRepository()
                        .setURI(siteConfig.getGitSource().getUri())
                        .setDirectory(gitRepoConfigDir.toFile())
                        .setBranchesToClone(gitSource.getBranches());
                if (!StringUtils.isEmpty(gitSource.getUsername())) {
                    UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider =
                            new UsernamePasswordCredentialsProvider(gitSource.getUsername(), gitSource.getPassword());
                    cloneCommandBuilder.setCredentialsProvider(usernamePasswordCredentialsProvider);
                } else if (gitSource.getPrivateKeyFile() != null) {

                    cloneCommandBuilder.setTransportConfigCallback( new TransportConfigCallback() {
                        @Override
                        public void configure( Transport transport ) {
                            if (transport instanceof SshTransport) {
                                SshSessionFactory sshSessionFactory = createSshSessionFactor(gitSource);
                                log.debug("Setting ssh session factory to [{}]", sshSessionFactory);

                                ((SshTransport) transport).setSshSessionFactory(sshSessionFactory);

                            }

//                            sshTransport.setSshSessionFactory( sshSessionFactory );
                        }
                    } );

                }
                Git call =  cloneCommandBuilder.call();
            }

//            for (String branchName : gitSource.getBranches()) {
//                LOGGER.debug("Checking out branch [{}] into [{}]", branchName, repoHostDir);
//            }

        } catch (Exception  e) {
//            log.error("An error has occured...", e);
            throw new RuntimeException(e);
        }

    }

    private SshSessionFactory createSshSessionFactor(GitConfig gitSource) {
        Path sshKeyPath;
        if (gitSource.getPrivateKeyFile() != null) {

            try {
                sshKeyPath = gitSource.getPrivateKeyFile().getFile().toPath();
                sshKeyPath = sshKeyPath.toAbsolutePath();
                log.debug("Using Private Key [{}] for ssh authentication", sshKeyPath);
                if (!Files.exists(sshKeyPath)) {
                    String error = String.format("The private key [%s] does not exist!", sshKeyPath);
                    throw new IllegalArgumentException(error);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final String sshKeyPathString = sshKeyPath.toAbsolutePath().toString();
            final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host hc, Session session) {
                    session.setConfig("StrictHostKeyChecking", "no");
                    log.debug("Server version [{}]", session.getServerVersion());


                    //session.setHostKeyAlias();
                }

                @Override
                protected JSch createDefaultJSch(FS fs) throws JSchException {
                    JSch jSch = super.createDefaultJSch(fs);
                    jSch.addIdentity(sshKeyPathString, gitSource.getPrivateKeyPassphrase());
                    log.debug("setting ssh identity key: [{}]", sshKeyPathString);
                    return jSch;
                }
            };
            return sshSessionFactory;

        } else {
            throw new IllegalArgumentException("SSH Key is null - but is not allowed to be null!");
        }
    }


    @Override
    public Stream<SiteResourceResolverRegistration> getResourceResolverRegistrations() {
        return config.getSites().stream()
                .filter(s -> s.getGitSource() != null)
                .map(this::createSiteResourceResolverRegistration);
    }

    private SiteResourceResolverRegistration createSiteResourceResolverRegistration(SiteConfig s) {
        SiteResourceResolverRegistration reg = new SiteResourceResolverRegistration();
        reg.setSiteName(s.getName());
        reg.setSiteResourceResolver(this);
        return reg;
    }

}
