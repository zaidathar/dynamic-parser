package com.zaidathar.dynamic.parser.config;
import com.zaidathar.dynamic.parser.interceptor.WebhookSecurityInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Logger log = LoggerFactory.getLogger(WebConfig.class);
    private final WebhookSecurityInterceptor webhookSecurityInterceptor;


    public WebConfig(WebhookSecurityInterceptor webhookSecurityInterceptor) {
        this.webhookSecurityInterceptor = webhookSecurityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(webhookSecurityInterceptor)
                .addPathPatterns("/webhook/github/config-reload");
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                log.info("Parser API call ... {}",request.getRemoteAddr());
                return HandlerInterceptor.super.preHandle(request, response, handler);
            }
        }).addPathPatterns("/api/parse");
        log.info("WebhookSecurityInterceptor registered for path /webhook/github/config-reload");
    }
}