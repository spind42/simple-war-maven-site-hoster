package at.stsp.dev.sitehoster.site;

import java.util.stream.Stream;

public interface SiteResourceResolverRegistrationProvider {

    Stream<SiteResourceResolverRegistration> getResourceResolverRegistrations();

}
