package com.paymentprocessor.analytics.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** S3 / MinIO clients, only wired when the S3 storage backend is selected. */
@Configuration
@ConditionalOnProperty(name = "analytics.storage.backend", havingValue = "s3")
public class S3Config {

    @Bean
    public S3Client s3Client(AnalyticsProperties props) {
        AnalyticsProperties.Storage.S3 s3 = props.getStorage().getS3();
        var builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (StringUtils.hasText(s3.getEndpoint())) {
            // MinIO / custom endpoint: path-style access
            builder.endpointOverride(URI.create(s3.getEndpoint()))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(AnalyticsProperties props) {
        AnalyticsProperties.Storage.S3 s3 = props.getStorage().getS3();
        var builder = S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (StringUtils.hasText(s3.getEndpoint())) {
            builder.endpointOverride(URI.create(s3.getEndpoint()));
        }
        return builder.build();
    }
}
