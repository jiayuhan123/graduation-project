package xyz.jiayuhan.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码编码器配置
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 强哈希密码编码器（仅依赖 spring-security-crypto，不引入 Spring Security 全家桶）
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
