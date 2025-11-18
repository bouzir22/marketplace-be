package com._ach.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com._ach.backend.repository")
public class ElasticsearchConfig {
    // Spring Boot auto-configuration handles the Elasticsearch client setup
    // based on application.properties settings
}
