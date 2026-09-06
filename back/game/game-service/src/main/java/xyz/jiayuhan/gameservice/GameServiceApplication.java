package xyz.jiayuhan.gameservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 游戏闯关服务启动入口。 */
@SpringBootApplication
@MapperScan("xyz.jiayuhan.gameservice.mapper")
public class GameServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}

