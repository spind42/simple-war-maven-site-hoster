package at.stsp.dev.sitehoster.site;

import at.stsp.dev.sitehoster.site.configuration.SiteConfig;
import at.stsp.dev.sitehoster.site.configuration.SiteHostAppConfigurationProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HttpServletBean;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class SiteHosterServlet extends HttpServletBean {

    @Autowired
    List<SiteResourceResolverRegistrationProvider> siteResourceResolverRegistrationProviders;

    @Autowired
    private SiteHostAppConfigurationProperties config;

    private Map<String, SiteResourceResolver> resourceResolverMap;

    @PostConstruct
    public void initRegistrationMap() {
        //noinspection LambdaBodyCanBeCodeBlock,Convert2MethodRef
        resourceResolverMap = siteResourceResolverRegistrationProviders
                .stream()
                .flatMap(r -> r.getResourceResolverRegistrations())
                .collect(Collectors.toMap(r -> r.getSiteName(), r -> r.getSiteResourceResolver()));
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        int servletPathLength = req.getServletPath().length();
        final String resourcePath = req.getRequestURI().substring(servletPathLength + 1);


        Optional<SiteConfig> siteConfigOptional = config.getSites()
                .stream()
                .filter(s -> resourcePath.startsWith(s.getName()))
                .findAny();

        ResourceQuery query = new ResourceQuery();

        if (siteConfigOptional.isPresent()) {
            SiteConfig siteConfig = siteConfigOptional.get();
            if (resourcePath.length() <= siteConfig.getName().length() + 1) {
                send404asError(resp, "Not resolvable");
                return;
            }
            String resourceName = resourcePath.substring(siteConfig.getName().length() + 1);
            SiteResourceResolver siteResourceResolver = resourceResolverMap.get(siteConfig.getName());
            if (siteResourceResolver == null) {
                this.send404asError(resp, String.format("No resource resolver registered for name [%s]", siteConfig.getName()));
                return;
            }
            if (StringUtils.isEmpty(resourceName)) {
                this.send404asError(resp, String.format("An empty resource name  [%s] cannot be resolved!", resourceName));
                return;
            }

            log.debug("Using site config name [{}] to provide resource [{}]", siteConfig.getName(), resourceName);
            try (OutputStream os = resp.getOutputStream()) {
                query.setResourcePath(resourceName);
                siteResourceResolver.copyIntoOuputStream(siteConfig, query, os);
            } catch (Exception e) {
                resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                throw e;
            }
        } else {
            resp.setStatus(HttpStatus.NOT_FOUND.value());
        }
    }

    private void send404asError(HttpServletResponse resp, String format) {
        log.error(format);
        resp.setStatus(HttpStatus.NOT_FOUND.value());
    }

}
