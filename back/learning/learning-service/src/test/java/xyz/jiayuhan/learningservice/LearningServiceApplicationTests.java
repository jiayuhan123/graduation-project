package xyz.jiayuhan.learningservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningServiceApplicationTests {

    @Test
    void declaresSpringBootApplication() {
        assertTrue(LearningServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}

