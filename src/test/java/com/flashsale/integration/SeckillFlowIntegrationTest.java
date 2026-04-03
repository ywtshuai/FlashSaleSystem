package com.flashsale.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class SeckillFlowIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("flash_sale_system")
            .withUsername("test")
            .withPassword("test123456");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("MASTER_DB_URL", MYSQL::getJdbcUrl);
        registry.add("MASTER_DB_USERNAME", MYSQL::getUsername);
        registry.add("MASTER_DB_PASSWORD", MYSQL::getPassword);
        registry.add("SLAVE_DB_URL", MYSQL::getJdbcUrl);
        registry.add("SLAVE_DB_USERNAME", MYSQL::getUsername);
        registry.add("SLAVE_DB_PASSWORD", MYSQL::getPassword);
        registry.add("REDIS_HOST", REDIS::getHost);
        registry.add("REDIS_PORT", () -> REDIS.getMappedPort(6379));
        registry.add("KAFKA_BOOTSTRAP_SERVERS", KAFKA::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteSeckillFlowEndToEnd() throws Exception {
        String username = "tc_user_" + System.currentTimeMillis();
        String password = "123456";

        JsonNode registerResponse = readJson(mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertEquals(200, registerResponse.get("code").asInt());

        JsonNode loginResponse = readJson(mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertEquals(200, loginResponse.get("code").asInt());
        String token = loginResponse.get("data").get("token").asText();
        assertTrue(token.startsWith("ey"));

        JsonNode seckillResponse = readJson(mockMvc.perform(post("/api/seckill/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertEquals(200, seckillResponse.get("code").asInt());
        assertEquals("QUEUING", seckillResponse.get("data").get("status").asText());

        JsonNode duplicateResponse = readJson(mockMvc.perform(post("/api/seckill/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertEquals(500, duplicateResponse.get("code").asInt());
        assertEquals("请勿重复秒杀", duplicateResponse.get("message").asText());

        JsonNode resultResponse = waitForSeckillResult(token);
        assertEquals("SUCCESS", resultResponse.get("data").get("status").asText());
        Long orderId = resultResponse.get("data").get("orderId").asLong();
        assertNotNull(orderId);
        assertTrue(orderId > 0L);

        JsonNode orderResponse = readJson(mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertEquals(200, orderResponse.get("code").asInt());
        assertEquals(orderId.longValue(), orderResponse.get("data").get("orderId").asLong());
        assertEquals(1L, orderResponse.get("data").get("productId").asLong());
    }

    private JsonNode waitForSeckillResult(String token) throws Exception {
        JsonNode latest = null;
        for (int i = 0; i < 20; i++) {
            latest = readJson(mockMvc.perform(get("/api/seckill/result")
                            .param("productId", "1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
            String status = latest.get("data").get("status").asText();
            if (!"QUEUING".equals(status) && !"EMPTY".equals(status)) {
                return latest;
            }
            Thread.sleep(500L);
        }
        return latest;
    }

    private JsonNode readJson(String content) throws Exception {
        return objectMapper.readTree(content);
    }
}
