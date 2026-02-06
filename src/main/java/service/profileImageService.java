package service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class profileImageService {

	private final Cloudinary cloudinary;

	public profileImageService(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	public String saveProfileImage(MultipartFile file) {
		if (file.isEmpty() || file == null) {
			throw new RuntimeException("File is empty");
		}
		try {
			// Determine resource type based on file content type or extension
			String contentType = file.getContentType();
			String resourceType = "auto"; // Default for images

			if (contentType != null) {
				if (contentType.startsWith("video/")) {
					resourceType = "video";
				} else if (contentType.equals("application/pdf")) {
					resourceType = "raw"; // For PDF documentation
				}
			}

			Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
					ObjectUtils.asMap("resource_type", resourceType));

			return (String) uploadResult.get("secure_url");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to Upload to Cloudinary");
		}
	}

	public String updateProfileImage(MultipartFile file, String oldFileUrl) {
		if (oldFileUrl != null && !oldFileUrl.isEmpty()) {
			deleteProfileImage(oldFileUrl);
		}
		return saveProfileImage(file);
	}

	public boolean deleteProfileImage(String fileUrl) {
		if (fileUrl == null || fileUrl.isEmpty()) {
			return false;
		}
		try {
			String publicId = extractPublicId(fileUrl);
			String resourceType = fileUrl.contains("/video/") ? "video" : fileUrl.contains("/raw/") ? "raw" : "image";

			Map<?, ?> result = cloudinary.uploader().destroy(publicId,
					ObjectUtils.asMap("resource_type", resourceType));

			return "ok".equals(result.get("result"));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private String extractPublicId(String url) {
		// Example:
		// https://res.cloudinary.com/cloudname/image/upload/v12345/public_id.jpg
		try {
			String[] parts = url.split("/");
			String lastPart = parts[parts.length - 1];
			int dotIndex = lastPart.lastIndexOf('.');
			if (dotIndex != -1) {
				return lastPart.substring(0, dotIndex);
			}
			return lastPart;
		} catch (Exception e) {
			return url;
		}
	}
}
