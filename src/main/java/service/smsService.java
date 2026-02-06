package service;

import org.springframework.stereotype.Service;


public interface smsService {
	void sendOtp(String phoneNumber, String otp);
}
