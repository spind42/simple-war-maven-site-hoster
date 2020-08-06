package at.stsp.dev.sitehoster.site;

import at.stsp.dev.sitehoster.site.configuration.SiteConfig;

import java.io.OutputStream;

public interface SiteResourceResolver {

    void copyIntoOuputStream(SiteConfig siteConfig, ResourceQuery query, OutputStream os);

}
