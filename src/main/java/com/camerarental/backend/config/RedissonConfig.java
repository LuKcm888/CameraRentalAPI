package com.camerarental.backend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic Redisson configuration.
 * Supports TLS (rediss://) required by Upstash and similar managed Redis providers.
 * Only activates when spring.data.redis.host is set.
 * Named "redisson" to prevent RedissonAutoConfigurationV2 from creating a duplicate bean.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean(name = "redisson", destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        String protocol = sslEnabled ? "rediss://" : "redis://";
        String address = protocol + redisHost + ":" + redisPort;

        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectTimeout(10_000)
                .setTimeout(3_000)
                .setRetryAttempts(3)
                .setRetryInterval(1_500);

        return Redisson.create(config);
    }
}
