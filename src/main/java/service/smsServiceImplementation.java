package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

@Service
public class smsServiceImplementation implements smsService {
    @Value("${twilio.account_sid:}")
    private String accountSid;

    @Value("${twilio.auth_token:}")
    private String authToken;

    @Value("${twilio.phone_number:}")
    private String fromNumber;

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        // Check if Twilio credentials are configured
        if (accountSid == null || accountSid.isEmpty() ||
                authToken == null || authToken.isEmpty() ||
                fromNumber == null || fromNumber.isEmpty()) {
            // SMS not configured - log and skip
            System.out.println("SMS Service: Twilio credentials not configured. OTP sending skipped.");
            return;
        }

        try {
            Twilio.init(accountSid, authToken);
            Message.creator(
                    new com.twilio.type.PhoneNumber(phoneNumber),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    "Your verification OTP is: " + otp + " (Valid for 5 minutes)").create();
            System.out.println("SMS Service: OTP sent successfully to " + phoneNumber);
        } catch (Exception e) {
            System.out.println("SMS Service: Failed to send OTP - " + e.getMessage());
        }
    }
}
