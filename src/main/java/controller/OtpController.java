package controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Repository.RepositorytwoRepo;
import io.micrometer.common.util.StringUtils;
import service.otpService;
import entity.Registration;
import entity.OtpClass;

@RestController
@RequestMapping("/auth")
public class OtpController {
    @Autowired
    private otpService otpService;

    @Autowired
    private service.RedisOTPService redisOtpService;

    @Autowired
    private RepositorytwoRepo regtwo;

    @PostMapping("/sendOtp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody OtpClass sms) {
        Map<String, Object> jsonResponse = new HashMap<>();
        String phone = sms.getPhoneNumber();

        try {
            if (StringUtils.isEmpty(phone)) {
                jsonResponse.put("Success", false);
                jsonResponse.put("Message", "Phone number cannot be empty");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResponse);
            } else {
                // Check if the phone number exists in the database
                Registration user = regtwo.findByPhoneNumber(phone);
                System.out.println(phone);
                if (user != null) {
                    jsonResponse.put("Success", false);
                    jsonResponse.put("Message", "This phone number is already registered");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonResponse);
                } else {
                    // Use Redis to generate and store OTP
                    String otp = redisOtpService.generateAndStoreOTP("registration(twilio)", phone);

                    // Send via Twilio (using the method we just updated in otpService)
                    otpService.sendSMSText(phone, otp);

                    jsonResponse.put("Success", true);
                    jsonResponse.put("Message", "OTP sent successfully via Twilio");
                    return ResponseEntity.ok(jsonResponse);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.put("Success", false);
            jsonResponse.put("Message", "Error: An error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResponse);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody OtpClass otp) {
        String userOtp = otp.getOtp();
        Map<String, Object> jsonResponse = new HashMap<>();

        try {
            if (StringUtils.isEmpty(userOtp)) {
                jsonResponse.put("Success", false);
                jsonResponse.put("Message", "OTP cannot be empty");
                return ResponseEntity.badRequest().body(jsonResponse);
            } else {
                // Verify using Redis Service
                boolean isValidOtp = redisOtpService.validateOTP("registration(twilio)", otp.getPhoneNumber(), userOtp);

                if (isValidOtp) {
                    jsonResponse.put("Success", true);
                    jsonResponse.put("Message", "Verified successfully");
                    return ResponseEntity.ok(jsonResponse);
                } else {
                    jsonResponse.put("Success", false);
                    jsonResponse.put("Message", "Verification failed");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResponse);
                }
            }
        } catch (Exception e) {
            jsonResponse.put("Success", false);
            jsonResponse.put("Message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResponse);
        }
    }
}