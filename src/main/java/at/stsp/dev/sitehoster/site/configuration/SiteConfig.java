package at.stsp.dev.sitehoster.site.configuration;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Validated
public class SiteConfig {

    @NotBlank
    @Pattern(regexp = "^\\w+$")
    private String name;

    private GitConfig gitSource;

}
