package at.stsp.dev.sitehoster.site;

import at.stsp.dev.sitehoster.site.configuration.SiteHostAppConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteHosterServletConfiguration {

    @Autowired
    SiteHostAppConfigurationProperties config;

    @Bean
    SiteHosterServlet siteServeServlet() {
        return new SiteHosterServlet();
    }

    @Bean
    ServletRegistrationBean<SiteHosterServlet> siteHostServletRegistration () {
        ServletRegistrationBean<SiteHosterServlet> registrationBean = new ServletRegistrationBean<>();
        registrationBean.addUrlMappings(config.getBaseUrl());
        registrationBean.setServlet(siteServeServlet());
        return registrationBean;
    }

}
