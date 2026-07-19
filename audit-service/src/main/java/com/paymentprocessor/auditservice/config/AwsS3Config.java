package com.paymentprocessor.auditservice.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Builds the S3 client used to write the legal copy to an Object Lock bucket.
 *
 * <p>Credentials come from the default provider chain (IAM role / instance profile /
 * environment) — never hard-coded. An optional endpoint override supports LocalStack
 * and other S3-compatible endpoints for local development and testing.
 */
@Configuration
@ConditionalOnProperty(prefix = "audit.s3", name = "enabled", havingValue = "true")
public class AwsS3Config {

    @Bean
    public S3Client s3Client(AuditProperties props) {
        AuditProperties.S3 s3 = props.getS3();

        var builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (StringUtils.hasText(s3.getEndpoint())) {
            // LocalStack / MinIO style endpoints need path-style access.
            builder.endpointOverride(URI.create(s3.getEndpoint()))
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true)
                           .build());
        }
        return builder.build();
    }
}
