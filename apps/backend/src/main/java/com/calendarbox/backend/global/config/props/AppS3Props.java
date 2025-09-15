package com.calendarbox.backend.global.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.s3")
public record AppS3Props(
        String bucket, String endpoint, String region,
        String accessKey, String secretKey,
        boolean usePathStyle, int presignMinutes, String thumbPrefix
) {}
