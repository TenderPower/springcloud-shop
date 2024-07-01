package net.xdclass;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//构造一个启动类
@SpringBootApplication
//加一个扫描包
// (@MapperScan 注解用于指定 MyBatis 应该扫描哪个包下的 Mapper 接口。这样，在该包下的所有接口都会被自动注册为 MyBatis 的 Mapper 接口，而无需在每个接口上都添加 @Mapper 注解)
@MapperScan("net.xdclass.mapper")
public class UserApplication {
    public static void main(String[] args) {

        SpringApplication.run(UserApplication.class,args);
    }
}
