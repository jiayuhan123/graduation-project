package xyz.jiayuhan.monomer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonolithApplicationArchitectureTest {

    @Test
    void hasOneBootEntryPointWithoutFeignClientScanning() {
        assertTrue(MonolithApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertFalse(MonolithApplication.class.isAnnotationPresent(EnableFeignClients.class));
    }
}
