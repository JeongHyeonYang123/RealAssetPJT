package com.ssafy.home.config;

import com.ssafy.home.interceptor.PerformanceInterceptor;
import com.ssafy.home.interceptor.SessionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MvcConfigure implements WebMvcConfigurer {
    private final PerformanceInterceptor pi;
    private final SessionInterceptor si;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pi);
//        registry.addInterceptor(si).addPathPatterns("/auth/**").excludePathPatterns("/auth/help");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/index").setViewName("index");
        registry.addRedirectViewController("/", "/index");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedOriginPatterns("http://localhost:3000")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
