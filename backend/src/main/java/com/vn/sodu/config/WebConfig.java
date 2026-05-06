package com.vn.sodu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Cho phép tất cả các endpoint bắt đầu bằng /api/
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:8081") // URL của Vite
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        registry.addMapping("/api/public/**")
                .allowedOrigins("*")
                .allowedMethods("GET")
                .allowCredentials(false);
        registry.addMapping("/api/v1/public/**")
                .allowedOrigins("*")
                .allowedMethods("GET")
                .allowCredentials(false);
        registry.addMapping("/api/auth/**")
                .allowedOrigins("*")
                .allowedMethods("GET","POST","PUT")
                .allowCredentials(false);
    }
}
