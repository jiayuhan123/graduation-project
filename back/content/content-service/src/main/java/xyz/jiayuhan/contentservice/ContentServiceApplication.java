package xyz.jiayuhan.contentservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.jiayuhan.userapi.client.UserFeignClient;

/** 内容题库服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.contentservice.mapper")
@EnableFeignClients(clients = UserFeignClient.class)
@EnableScheduling
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }
}
