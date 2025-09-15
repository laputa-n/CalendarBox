package com.calendarbox.backend.global.infra.storage;

import com.calendarbox.backend.global.config.props.AppS3Props;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(AppS3Props.class)
public class S3Config {
    @Bean
    S3Client s3(AppS3Props p) {
        var b = S3Client.builder().region(Region.of(p.region()));
        if (p.endpoint()!=null && !p.endpoint().isBlank()) {
            b = b.endpointOverride(URI.create(p.endpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(p.usePathStyle()).build())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(p.accessKey(), p.secretKey())));
        }
        return b.build();
    }
    @Bean
    S3Presigner s3Presigner(AppS3Props p) {
        var b = S3Presigner.builder().region(Region.of(p.region()));
        if (p.endpoint()!=null && !p.endpoint().isBlank()) {
            b = b.endpointOverride(URI.create(p.endpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(p.accessKey(), p.secretKey())));
        }
        return b.build();
    }
}
