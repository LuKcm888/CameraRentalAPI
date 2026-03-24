package com.camerarental.backend.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtBlacklistServiceTest {

    @Test
    @DisplayName("In-memory blacklist expires at TTL boundary")
    void inMemoryBlacklistExpiresOnBoundary() throws Exception {
        JwtBlacklistService service = new JwtBlacklistService(5); // 5 ms TTL

        service.blacklist("jti-1");
        assertThat(service.isBlacklisted("jti-1")).isTrue();

        Thread.sleep(10); // allow TTL to elapse

        assertThat(service.isBlacklisted("jti-1")).isFalse();
    }

    @Test
    @DisplayName("Redis path stores and reads blacklist entries")
    void redisWriteAndRead() {
        RedissonClient client = mock(RedissonClient.class);
        RKeys keys = mock(RKeys.class);
        @SuppressWarnings("unchecked")
        RBucket<Boolean> bucket = mock(RBucket.class);
        when(client.getKeys()).thenReturn(keys);
        when(keys.count()).thenReturn(1L);
        when(client.<Boolean>getBucket("jwt-blacklist:jti-redis")).thenReturn(bucket);
        when(bucket.get()).thenReturn(Boolean.TRUE);

        JwtBlacklistService service = new JwtBlacklistService(1000);
        ReflectionTestUtils.setField(service, "redissonClient", client);
        service.init();

        service.blacklist("jti-redis");

        verify(bucket).set(eq(Boolean.TRUE), eq(1000L), eq(TimeUnit.MILLISECONDS));
        assertThat(service.isBlacklisted("jti-redis")).isTrue();
    }

    @Test
    @DisplayName("Redis failures fall back to memory")
    void redisFailureFallsBackToMemory() {
        RedissonClient client = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<Boolean> bucket = mock(RBucket.class);
        when(client.<Boolean>getBucket("jwt-blacklist:jti-fail")).thenReturn(bucket);
        // fail writes
        doThrow(new RuntimeException("write boom")).when(bucket).set(any(), anyLong(), any());

        JwtBlacklistService service = new JwtBlacklistService(1000);
        ReflectionTestUtils.setField(service, "redissonClient", client);
        service.init();

        service.blacklist("jti-fail"); // write failure -> falls back to memory

        // now force read failure, which should trigger memory fallback and still return true
        doThrow(new RuntimeException("read boom")).when(bucket).get();
        assertThat(service.isBlacklisted("jti-fail")).isTrue();
    }
}
