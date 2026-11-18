package com.suwontrip.app.catalog.config;

import com.suwontrip.app.catalog.Catalog;
import com.suwontrip.app.catalog.CatalogLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.catalog", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CatalogProperties.class)
public class CatalogConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CatalogConfiguration.class);

    @Bean
    @Lazy(false)
    Catalog catalog(CatalogProperties properties) {
        String configuredPath = properties.path();

        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException(
                    "app.catalog.path is required when app.catalog.enabled=true"
            );
        }

        Path path;
        try {
            path = Path.of(configuredPath);
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(
                    "app.catalog.path is not a valid filesystem path",
                    exception
            );
        }

        Catalog catalog = new CatalogLoader().load(path);
        log.info("Catalog loaded: schemaVersion={}, cafes={}, menus={}",
                catalog.schemaVersion(), catalog.cafes().size(), catalog.menus().size());
        return catalog;
    }
}
