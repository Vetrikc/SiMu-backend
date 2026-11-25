package org.example.simu.playlist;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A playlist owned by one user must not be readable by another (IDOR regression test).
 */
@Tag("integration")
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:1"
})
@AutoConfigureMockMvc
@Testcontainers
class PlaylistOwnershipIT {

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

    long playlistId;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("delete from playlist_track");
        jdbcTemplate.update("delete from playlists");
        jdbcTemplate.update(
                "insert into playlists (name, description, user_id, created_at, updated_at) values (?, ?, ?, now(), now())",
                "Owner playlist", "secret", 1L);
        playlistId = jdbcTemplate.queryForObject("select id from playlists where user_id = 1", Long.class);
    }

    @Test
    void ownerCanRead() throws Exception {
        mockMvc.perform(get("/api/v1/playlists/" + playlistId)
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "owner")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void strangerGetsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/playlists/" + playlistId)
                        .header("X-User-Id", "2")
                        .header("X-User-Name", "stranger")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isNotFound());
    }
}