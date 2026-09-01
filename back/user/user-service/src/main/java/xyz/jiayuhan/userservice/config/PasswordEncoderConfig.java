package xyz.jiayuhan.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 密码哈希组件配置。 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 创建 BCrypt 强哈希编码器。
     *
     * <p>工作因子可按部署机器性能调整；这里只使用独立加密组件，不引入 Spring Security
     * 的过滤器链，实际会话和鉴权仍由 Sa-Token 负责。</p>
     *
     * @param strength BCrypt 工作因子
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder(
            @Value("${security.password.bcrypt-strength:12}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }
}
