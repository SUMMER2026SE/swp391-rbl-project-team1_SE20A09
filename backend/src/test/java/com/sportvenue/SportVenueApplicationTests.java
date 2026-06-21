package com.sportvenue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — kiểm tra Spring ApplicationContext khởi động được.
 *
 * src/test/resources/application.yml được load thay cho
 * src/main/resources/application.yml khi chạy test, loại bỏ
 * spring.config.import (dotenv) và dùng đúng port CI (5432).
 *
 * Cần CI services:  postgres:5432, redis:6379
 * Env vars CI:      SPRING_DATASOURCE_URL, SPRING_DATA_REDIS_HOST, JWT_SECRET
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SportVenueApplicationTests {

    @Test
    void contextLoads() {
        // ApplicationContext phải khởi động không ném exception.
    }
}
