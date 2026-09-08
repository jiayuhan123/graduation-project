package xyz.jiayuhan.auditservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import xyz.jiayuhan.userapi.client.UserFeignClient;

/** 通用审计服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.auditservice.mapper")
@EnableFeignClients(clients = UserFeignClient.class)
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
