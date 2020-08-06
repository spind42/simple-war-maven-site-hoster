package at.stsp.dev.sitehoster.site.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import java.util.List;

@ConfigurationProperties(prefix = "sitehoster")
@Component
@Validated
@Data
public class SiteHostAppConfigurationProperties {

    /**
     * Base URL under which the sites are hosted...
     */
    @NotBlank
    private String baseUrl = "/site/*";

    private List<SiteConfig> sites;

    @NotBlank
    private String tempDir = "./temp";


}
