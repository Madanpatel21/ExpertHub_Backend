package service;

import com.google.auth.oauth2.AccessToken;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Service
public class GoogleAuthService {

    @Value("${google.service.account.json.path}")
    private String serviceAccountJsonPath;

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private final ResourceLoader resourceLoader;

    public GoogleAuthService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String generateAccessToken() throws IOException {
        Resource resource;

        // Check if path starts with 'classpath:', otherwise treat it as a file path
        if (serviceAccountJsonPath.startsWith("classpath:")) {
            resource = new ClassPathResource(serviceAccountJsonPath.replace("classpath:", ""));
        } else {
            // For file system path
            resource = resourceLoader.getResource(serviceAccountJsonPath);
        }

        // Check if the resource exists
        if (!resource.exists()) {
            throw new IOException("Service account JSON file not found: " + serviceAccountJsonPath);
        }

        // Try to load the credentials from the resource
        try (InputStream inputStream = resource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(inputStream)
                    .createScoped(Collections.singleton(CLOUD_PLATFORM_SCOPE));

            // Refresh the token if it's expired
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();

            return token.getTokenValue();
        } catch (IOException e) {
            throw new IOException("Failed to generate access token: " + e.getMessage(), e);
        }
    }
}