package xyz.jiayuhan.fileservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.userapi.client.UserFeignClient;

/** 通用文件服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.fileservice.mapper")
@EnableFeignClients(clients = {UserFeignClient.class, TeachingFeignClient.class})
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
