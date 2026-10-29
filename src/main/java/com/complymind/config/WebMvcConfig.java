package com.complymind.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private NotificationInterceptor notificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(notificationInterceptor)
                .excludePathPatterns("/login", "/setup", "/setup/**", "/forgot-password", "/reset-password", "/reset-password/**", "/api/auth/**", "/css/**", "/js/**", "/images/**", "/favicon.ico");
    }
}
