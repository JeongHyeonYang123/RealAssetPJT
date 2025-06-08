package com.ssafy.home.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "SSAFY HOME API 명세서", description = "SSAFY HOME API 명세서", version = "v1"))
public class SwaggerConfig {
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .info(new io.swagger.v3.oas.models.info.Info().title("API 문서").version("1.0.0"));
    }

    @Bean
    GroupedOpenApi userOpenApi() {
        String[] paths = {"/api/v1/users/**"};
        return GroupedOpenApi.builder()
                .group("사용자 관련 API")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    GroupedOpenApi boardOpenApi() {
        String[] paths = {"/api/v1/boards/**"};
        return GroupedOpenApi.builder()
                .group("게시글 관련 API")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    GroupedOpenApi interestAreaOpenApi() {
        String[] paths = {"/api/v1/interest-areas/**"};
        return GroupedOpenApi.builder()
                .group("관심지역 관련 API")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    GroupedOpenApi transactionOpenApi() {
        String[] paths = {"/api/v1/transactions/**"};
        return GroupedOpenApi.builder()
                .group("아파트 거래 내역 관련 API")
                .pathsToMatch(paths)
                .build();
    }
    @Bean
    GroupedOpenApi houseOpenApi() {
        String[] paths = {"/api/v1/house/**"};
        return GroupedOpenApi.builder()
                .group("아파트 거래 내역 관련 API 2")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    GroupedOpenApi commercialAreaOpenApi() {
        String[] paths = {"/api/v1/commercial-areas/**"};
        return GroupedOpenApi.builder()
                .group("상권 정보 관련 API")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    GroupedOpenApi admOpenApi() {
        String[] paths = {"/api/v1/adm/**"};
        return GroupedOpenApi.builder()
                .group("시구군동 조회 API")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    GroupedOpenApi adminOpenApi() {
        String[] paths = {"/api/v1/admin/**"};
        return GroupedOpenApi.builder()
                .group("관리자 API")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    GroupedOpenApi chatbotOpenApi() {
        String[] paths = {"/api/v1/chatbot/**"};
        return GroupedOpenApi.builder()
                .group("챗봇 API")
                .pathsToMatch(paths)
                .build();
    }
}
