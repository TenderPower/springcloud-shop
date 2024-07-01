package net.xdclass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ConfigurationProperties 注解用于将应用程序配置文件（如 application.properties 或 application.yml）中的配置属性绑定到 Java 对象。
 * 被注解的类应该定义与配置属性同名的字段，Spring Boot 会自动将配置文件中的值映射到对应的字段中
 */
@ConfigurationProperties(prefix = "aliyun.oss")
@Configuration
@Data
public class OSSConfig {
    //    配置文档中的横杠 会 自动转为 驼峰形式
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
