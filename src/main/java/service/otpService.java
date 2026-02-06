package service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class otpService {

    @Value("${msg91.authkey}")
    private String authKey;

    @Value("${msg91.template_id}")
    private String templateId;

    @Value("${msg91.url.verify}")
    private String msg91VerifyUrl;

    @Value("${msg91.url.send}")
    private String msg91SendUrl;

    @Value("${msg91.url.resend}")
    private String msg91ResendUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @org.springframework.beans.factory.annotation.Autowired
    private smsService SmsService;

    // ✅ Method to send OTP (MSG91 Internal Generation)
    public String sendOTP(String mobileNumber) {
        String formattedNumber = formatPhoneNumber(mobileNumber);

        String requestUrl = msg91SendUrl
                + "?otp_length=6&otp_expiry=10"
                + "&template_id=" + templateId
                + "&mobile=" + formattedNumber
                + "&authkey=" + authKey;

        return restTemplate.getForObject(requestUrl, String.class);
    }

    // ✅ Method to verify OTP (MSG91 Internal Validation)
    public boolean verifyOTP(String mobileNumber, String otp) {
        String formattedNumber = formatPhoneNumber(mobileNumber);

        String requestUrl = msg91VerifyUrl
                + "?otp=" + otp
                + "&mobile=" + formattedNumber
                + "&authkey=" + authKey;

        try {
            String response = restTemplate.getForObject(requestUrl, String.class);
            System.out.println("MSG91 Response: " + response);

            JSONObject jsonResponse = new JSONObject(response);
            return jsonResponse.optString("type").equals("success");
        } catch (Exception e) {
            System.err.println("Error verifying OTP: " + e.getMessage());
            return false;
        }
    }

    // ✅ Method to resend OTP
    public String resendOTP(String mobileNumber, String retryType) {
        String formattedNumber = formatPhoneNumber(mobileNumber);

        String requestUrl = msg91ResendUrl
                + "?mobile=" + formattedNumber
                + "&authkey=" + authKey
                + "&retrytype=" + retryType;

        System.out.println("Resend OTP URL: " + requestUrl);
        return restTemplate.getForObject(requestUrl, String.class);
    }

    // ✅ Method to send a custom SMS message (used for Redis-generated OTPs)
    public void sendSMSText(String mobileNumber, String message) {
        String formattedNumber = formatPhoneNumber(mobileNumber);
        System.out.println("DEBUG [Twilio SMS] Sending to: " + formattedNumber + " | Message: " + message);

        // Use the injected Twilio service
        try {
            SmsService.sendOtp(formattedNumber, message);
        } catch (Exception e) {
            System.err.println("Failed to send Twilio SMS: " + e.getMessage());
        }
    }

    // ✅ Utility function to remove `+` from the mobile number
    private String formatPhoneNumber(String phoneNumber) {
        return phoneNumber.replace("+", "").trim();
    }
}