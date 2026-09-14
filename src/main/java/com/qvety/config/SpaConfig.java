package com.qvety.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the Angular build from the jar. A path that matches a real file (js, css, fonts, i18n json)
 * is served as-is; anything else (/clients/123, /login) gets index.html so deep links work.
 * /api, /actuator, /v3 and /swagger-ui are handled by their controllers before this resolver.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    var requested = location.createRelative(resourcePath);
                    if (requested.exists() && requested.isReadable()) {
                        return requested;
                    }
                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")
                            || resourcePath.startsWith("v3/") || resourcePath.startsWith("swagger-ui")) {
                        return null;
                    }
                    // a missing file with an extension is a real 404, not a route
                    var last = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                    if (last.contains(".")) {
                        return null;
                    }
                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}
