package xyz.jiayuhan.learningservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.userapi.client.UserFeignClient;

/** 学习闭环服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.learningservice.mapper")
@EnableFeignClients(clients = {UserFeignClient.class, ContentFeignClient.class, TeachingFeignClient.class})
public class LearningServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningServiceApplication.class, args);
    }
}

