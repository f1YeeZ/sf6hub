package com.example.hubdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.CacheControl;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String uploadDir;
    private final String storageProvider;
    private final String[] allowedOriginPatterns;

    public WebConfig(@Value("${app.upload-dir}") String uploadDir,
                     @Value("${app.storage.provider:local}") String storageProvider,
                     @Value("${app.cors.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173}") String[] allowedOriginPatterns) {
        this.uploadDir = uploadDir;
        this.storageProvider = storageProvider;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-CSRF-Token")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!"local".equalsIgnoreCase(storageProvider)) return;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
