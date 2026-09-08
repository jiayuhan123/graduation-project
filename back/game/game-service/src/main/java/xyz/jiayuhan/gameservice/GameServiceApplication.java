package xyz.jiayuhan.gameservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.userapi.client.UserFeignClient;

/** 游戏闯关服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.gameservice.mapper")
@EnableFeignClients(clients = {ContentFeignClient.class, UserFeignClient.class})
@EnableScheduling
public class GameServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
