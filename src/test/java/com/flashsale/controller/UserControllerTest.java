package com.flashsale.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.config.WebMvcConfig;
import com.flashsale.dto.LoginRequest;
import com.flashsale.dto.RegisterRequest;
import com.flashsale.dto.UserInfoResponse;
import com.flashsale.dto.UserLoginResponse;
import com.flashsale.handler.GlobalExceptionHandler;
import com.flashsale.interceptor.JwtAuthInterceptor;
import com.flashsale.mapper.UserMapper;
import com.flashsale.properties.JwtProperties;
import com.flashsale.service.UserService;
import com.flashsale.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, JwtAuthInterceptor.class, UserControllerTest.JwtTestConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=FlashSaleSystemJwtSecretKey2026FlashSaleSystem",
        "jwt.expiration=7200000"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("test1");
        request.setPassword("123456");

        when(userService.register(any(RegisterRequest.class))).thenReturn(1L);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1L));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("test1");
        request.setPassword("123456");

        UserLoginResponse response = new UserLoginResponse();
        response.setUserId(1L);
        response.setUsername("test1");
        response.setToken("mock-jwt-token");
        response.setExpireAt(System.currentTimeMillis() + 7200000);

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }

    @Test
    void shouldRejectMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("未登录或Token格式错误"));
    }

    @Test
    void shouldGetCurrentUserWithValidToken() throws Exception {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(1L);
        response.setUsername("test1");
        response.setCreateTime(LocalDateTime.of(2026, 3, 15, 10, 0));

        when(userService.getCurrentUser(1L)).thenReturn(response);

        String token = jwtUtil.generateToken(1L, "test1");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.username").value("test1"));
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret("FlashSaleSystemJwtSecretKey2026FlashSaleSystem");
            properties.setExpiration(7200000L);
            return properties;
        }

        @Bean
        JwtUtil jwtUtil(JwtProperties jwtProperties) {
            return new JwtUtil(jwtProperties);
        }
    }
}