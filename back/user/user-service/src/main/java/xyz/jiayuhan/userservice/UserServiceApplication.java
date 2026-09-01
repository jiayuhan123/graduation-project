package xyz.jiayuhan.userservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 通用用户服务启动入口，并集中扫描本服务的 MyBatis Mapper。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.userservice.mapper")
@EnableScheduling
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
