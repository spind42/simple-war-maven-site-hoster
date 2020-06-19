package at.stsp.dev.sitehoster;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


@SpringBootApplication
public class SiteHostApp extends SpringBootServletInitializer {

    private List<String> springConfigLocations = new ArrayList<String>();
    private Properties properties = new Properties();

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        SiteHostApp app = new SiteHostApp();
        app.configure(builder);
        builder.run(args);
    }

    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);
        String ctxPath = servletContext.getContextPath();

        String catalinaHome = System.getProperty("catalina.home");

        if (!StringUtils.isEmpty(catalinaHome)) {
            springConfigLocations.add(String.format("file:%s/config/%s", catalinaHome, ctxPath));
            springConfigLocations.add(String.format("file:%s/conf/%s", catalinaHome, ctxPath));
        }

    }

    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        builder.sources(SiteHostApp.class);

        springConfigLocations.add("file:/config/");
        springConfigLocations.add( "classpath:/config/");

        String configLocations = String.join(",", springConfigLocations);
        if (System.getProperty("spring.config.location") == null) {
            properties.put("spring.config.location", configLocations);
        }

        builder.properties(properties);
        return builder;
    }

}
