package com.calendarbox.backend.global.config;

import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;

@Configuration
public class SwaggerErrorResponsesConfig {

    @Bean
    public OperationCustomizer defaultErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            // 성공 응답만 있고 에러가 비어있으면 기본 에러를 주입
            addError(operation, "400", "Bad Request", exampleJson("VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", "/api/..."));
            addError(operation, "401", "Unauthorized", exampleJson("AUTH_INVALID_TOKEN", "인증 토큰이 유효하지 않습니다.", "/api/..."));
            addError(operation, "403", "Forbidden", exampleJson("AUTH_FORBIDDEN", "접근 권한이 없습니다.", "/api/..."));
            addError(operation, "500", "Internal Server Error", exampleJson("INTERNAL_ERROR", "서버에서 오류가 발생했습니다.", "/api/..."));

            return operation;
        };
    }

    private void addError(io.swagger.v3.oas.models.Operation operation, String code, String desc, String exampleJson) {
        if (operation.getResponses() != null && operation.getResponses().get(code) != null) return;

        var schema = new Schema<>().$ref("#/components/schemas/ErrorBody");

        var mediaType = new io.swagger.v3.oas.models.media.MediaType()
                .schema(schema)
                .example(exampleJson);

        var content = new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE, mediaType);

        operation.getResponses().addApiResponse(code, new ApiResponse().description(desc).content(content));
    }

    private String exampleJson(String code, String message, String path) {
        return """
               {
                 "code": "%s",
                 "message": "%s",
                 "timestamp": "2025-01-01T00:00:00Z",
                 "path": "%s"
               }
               """.formatted(code, message, path);
    }
}
