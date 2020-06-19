package at.stsp.dev.sitehoster.site.configuration;

import lombok.Data;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.util.regex.Pattern;

@Data
public class GitConfig {

    @NotBlank
    private String uri;

    private String username;

    private String password;

    private Resource privateKeyFile;

    private String privateKeyPassphrase;

    private Pattern branchFilter = Pattern.compile("master");

}
