package com.Expert_hub;

import Repository.roleRepository;
import Repository.userRepository;
import dto.loginRequest;
import dto.otpVerifyRequest;
import dto.registerRequest;
import entity.roles;
import entity.user;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private roleRepository roleRepo;

    @Autowired
    private userRepository userRepo;

    @BeforeEach
    void setUp() {
        userRepo.deleteAll();
        roleRepo.deleteAll();

        // Initialize Roles
        roles clientRole = new roles();
        clientRole.setName("CLIENT");
        roleRepo.save(clientRole);

        roles lawyerRole = new roles();
        lawyerRole.setName("LAWYER");
        roleRepo.save(lawyerRole);
    }

    @Test
    void testUserRegistrationAndLoginFlow() throws Exception {
        // 1. Register
        registerRequest regRequest = new registerRequest();
        regRequest.setName("Test User");
        regRequest.setEmail("test@example.com");
        regRequest.setPassword("Password123");
        regRequest.setPhoneNumber("1234567890");
        regRequest.setRole("CLIENT");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // 2. Login
        loginRequest logRequest = new loginRequest();
        logRequest.setEmail("test@example.com");
        logRequest.setPassword("Password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login Successfully"));

        // 3. Verify OTP (Since we can't easily get the OTP from the console/db here
        // without mocking service,
        // we'll check if the user exists and has an OTP)
        user registeredUser = userRepo.findByEmail("test@example.com").orElseThrow();
        String otp = registeredUser.getOtp();

        otpVerifyRequest verifyRequest = new otpVerifyRequest();
        verifyRequest.setPhoneNumber("1234567890"); // Note: Verify OTP uses phone number in this backend
        verifyRequest.setOtp(otp);

        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login Successful"));
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        loginRequest logRequest = new loginRequest();
        logRequest.setEmail("nonexistent@example.com");
        logRequest.setPassword("WrongPass");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logRequest)))
                .andExpect(status().is4xxClientError());
    }
}
