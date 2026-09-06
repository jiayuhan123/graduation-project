package xyz.jiayuhan.teachingservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.userapi.client.UserFeignClient;

/** 教学服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.teachingservice.mapper")
@EnableFeignClients(clients = {UserFeignClient.class, ContentFeignClient.class})
@EnableScheduling
public class TeachingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeachingServiceApplication.class, args);
    }
}
