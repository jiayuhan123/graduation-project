package xyz.jiayuhan.fileservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 通用文件服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.fileservice.mapper")
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
