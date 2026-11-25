package org.example.simu.music;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Keyset pagination against a real PostgreSQL (Liquibase schema included).
 */
@Tag("integration")
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:1"
})
@AutoConfigureMockMvc
@Testcontainers
class MusicPaginationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @org.springframework.test.context.DynamicPropertySource
    static void redisProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("delete from music");
        for (int i = 1; i <= 25; i++) {
            jdbcTemplate.update(
                    "insert into music (name, artist, url, created_at, updated_at) values (?, ?, ?, now(), now())",
                    "Track " + i, "Artist " + i, "http://example.com/" + i + ".mp3");
        }
    }

    @Test
    void returnsFirstPageAndCursor() throws Exception {
        mockMvc.perform(get("/api/v1/music").param("limit", "10")
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "tester")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNumber());
    }

    @Test
    void secondPageContinuesAfterCursor() throws Exception {
        String body = mockMvc.perform(get("/api/v1/music").param("limit", "10")
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "tester")
                        .header("X-User-Roles", "USER"))
                .andReturn().getResponse().getContentAsString();
        long cursor = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).get("nextCursor").asLong();

        mockMvc.perform(get("/api/v1/music").param("limit", "10").param("cursor", String.valueOf(cursor))
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "tester")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.items[0].id").value((int) (cursor + 1)));
    }

    @Test
    void searchFindsTracksByName() throws Exception {
        mockMvc.perform(get("/api/v1/music/search").param("q", "Track 2")
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "tester")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].name").value(org.hamcrest.Matchers.containsString("Track 2")));
    }
}