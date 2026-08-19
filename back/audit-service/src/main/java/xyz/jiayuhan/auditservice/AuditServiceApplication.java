package xyz.jiayuhan.auditservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 通用审计服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.auditservice.mapper")
public class AuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
