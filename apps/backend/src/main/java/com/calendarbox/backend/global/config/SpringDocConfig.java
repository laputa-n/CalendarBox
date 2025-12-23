package com.calendarbox.backend.global.config;

import com.calendarbox.backend.global.error.ErrorBody;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI().components(new Components()); // ✅ components 초기화

        var schemas = ModelConverters.getInstance().read(ErrorBody.class);

        var errorSchema = schemas.get("ErrorBody");
        if (errorSchema != null) {
            openAPI.getComponents().addSchemas("ErrorBody", errorSchema);
        } else {
            // 혹시 키가 다르게 생성되는 경우 대비
            schemas.forEach((name, schema) -> {
                if (schema != null && (name.endsWith("ErrorBody") || name.contains("ErrorBody"))) {
                    openAPI.getComponents().addSchemas("ErrorBody", schema);
                }
            });
        }

        return openAPI;
    }
}
