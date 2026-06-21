package com.sportvenue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — kiểm tra Spring ApplicationContext khởi động được.
 *
 * Dùng @TestPropertySource để:
 * 1. Vô hiệu hóa spring.config.import (tránh load .env file local
 *    ghi đè SPRING_DATASOURCE_URL về port 5433).
 * 2. Đảm bảo Flyway + JPA dùng đúng URL được truyền qua env vars CI.
 *
 * CI (ci-backend.yml) truyền env vars:
 *   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sportvenue_test
 *   SPRING_DATA_REDIS_HOST=localhost
 *   JWT_SECRET=...
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Tắt spring.config.import để .env file local không override
        // các env vars được CI truyền vào
        "spring.config.import=",
        // Fallback an toàn nếu env var chưa được set (local dev không có DB test)
        "spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/sportvenue_test}",
        "spring.datasource.username=${SPRING_DATASOURCE_USERNAME:sportvenue_user}",
        "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:testpassword}",
        "spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}",
        "spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}",
        "app.mail.mock=true",
        "JWT_SECRET=${JWT_SECRET:test_secret_key_for_ci_only_not_for_production_padding}"
})
class SportVenueApplicationTests {

    @Test
    void contextLoads() {
        // Verify Spring ApplicationContext khởi động không ném exception.
    }
}
