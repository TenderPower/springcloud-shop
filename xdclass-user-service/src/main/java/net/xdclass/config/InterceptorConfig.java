package net.xdclass.config;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 构建拦截器的配置类（配置文件）
 */
@Configuration
@Slf4j
public class InterceptorConfig implements WebMvcConfigurer {

    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        绑定自定义的拦截器
        registry.addInterceptor(loginInterceptor())
//                拦截要拦截的路径
                .addPathPatterns("/api/user/*/**", "/api/address/*/**")
//                 排除 不拦截的路径
                .excludePathPatterns("/api/notify/*/**", "/api/user/*/register",
                        "/api/user/*/login", "/api/user/*/upload");
    }



}
