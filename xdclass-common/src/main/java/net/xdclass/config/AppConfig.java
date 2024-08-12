package net.xdclass.config;

import feign.RequestInterceptor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局配置文件
 */
@Configuration
@Data
@Slf4j
public class AppConfig {
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private String redisPort;

    @Value("${spring.redis.password}")
    private String redisPwd;

    /**
     * 配置分布式锁redisson
     * @return
     */
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
//        单机模式
        config.useSingleServer().setPassword(redisPwd).setAddress("redis://"+redisHost+":"+redisPort);
        RedissonClient redisson = Redisson.create(config);
        return  redisson;
    }

    /**
     * 只是在微服务之间的请求前，加入拦截器
     * 解决，Feign调用丢失token的解决方法
     *
     * PS：这样，当使用Feign调用其他微服务的方法是，会在请求的时候，带上了token
     * @return
     */
    @Bean
    public RequestInterceptor requestInterceptor(){
        return requestTemplate -> {
//           获取别人向你请求的属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null){
//                获取别人向你请求
                HttpServletRequest request = attributes.getRequest();
                if (request == null) return;
                String token = request.getHeader("token");
//                你给别人发送请求时，带上token
                requestTemplate.header("token",token);
            }
        };
    }
}
