package xyz.jiayuhan.monomer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "xyz.jiayuhan.monomer",
        "xyz.jiayuhan.userservice",
        "xyz.jiayuhan.contentservice",
        "xyz.jiayuhan.learningservice",
        "xyz.jiayuhan.teachingservice",
        "xyz.jiayuhan.gameservice",
        "xyz.jiayuhan.fileservice",
        "xyz.jiayuhan.notificationservice",
        "xyz.jiayuhan.auditservice"
})
@MapperScan({
        "xyz.jiayuhan.userservice.mapper",
        "xyz.jiayuhan.contentservice.mapper",
        "xyz.jiayuhan.learningservice.mapper",
        "xyz.jiayuhan.teachingservice.mapper",
        "xyz.jiayuhan.gameservice.mapper",
        "xyz.jiayuhan.fileservice.mapper",
        "xyz.jiayuhan.notificationservice.mapper",
        "xyz.jiayuhan.auditservice.mapper"
})
@EnableScheduling
public class MonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }
}
