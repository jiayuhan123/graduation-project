package xyz.jiayuhan.learningservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 学习闭环服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.learningservice.mapper")
public class LearningServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningServiceApplication.class, args);
    }
}

