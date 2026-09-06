package xyz.jiayuhan.gameservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceApplicationTests {

    @Test
    void declaresSpringBootApplication() {
        assertTrue(GameServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}

