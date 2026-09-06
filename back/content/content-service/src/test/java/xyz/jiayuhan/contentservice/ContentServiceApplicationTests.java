package xyz.jiayuhan.contentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentServiceApplicationTests {

    @Test
    void declaresSpringBootApplication() {
        assertTrue(ContentServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}

