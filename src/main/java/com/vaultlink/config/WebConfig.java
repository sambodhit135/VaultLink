package com.vaultlink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve all static files
        registry
            .addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(0); // no caching in dev
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Root → login page
        registry.addViewController("/")
            .setViewName("forward:/login.html");
        // These allow direct URL access
        registry.addViewController("/dashboard")
            .setViewName("forward:/dashboard.html");
        registry.addViewController("/profile")
            .setViewName("forward:/profile.html");
        registry.addViewController("/documents")
            .setViewName("forward:/documents.html");
        registry.addViewController("/notifications")
            .setViewName("forward:/notifications.html");
    }
}
