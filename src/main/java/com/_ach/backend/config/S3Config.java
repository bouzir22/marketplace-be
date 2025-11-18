package com._ach.backend.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS S3 configuration for image storage.
 * Supports both AWS S3 and S3-compatible services (like MinIO, DigitalOcean Spaces).
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    /**
     * Create AmazonS3 client bean.
     * Only created when S3 is enabled in configuration.
     */
    @Bean
    @ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
    public AmazonS3 amazonS3() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));

        // If endpoint is specified, use it (for S3-compatible services like MinIO)
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpoint, region)
            ).withPathStyleAccessEnabled(true);
        } else {
            // Use standard AWS S3
            builder.withRegion(region);
        }

        return builder.build();
    }

    /**
     * Fallback bean when S3 is disabled.
     * Returns null but prevents dependency injection errors.
     */
    @Bean
    @ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "false", matchIfMissing = true)
    public AmazonS3 amazonS3Disabled() {
        return null;
    }
}
