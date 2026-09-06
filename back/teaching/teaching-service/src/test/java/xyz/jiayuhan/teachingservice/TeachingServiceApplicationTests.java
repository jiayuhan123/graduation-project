package xyz.jiayuhan.teachingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TeachingServiceApplicationTests {

    @Test
    void declaresSpringBootApplication() {
        assertTrue(TeachingServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}

