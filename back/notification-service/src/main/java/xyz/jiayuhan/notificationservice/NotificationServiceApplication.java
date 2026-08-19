package xyz.jiayuhan.notificationservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 通用通知服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.notificationservice.mapper")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
