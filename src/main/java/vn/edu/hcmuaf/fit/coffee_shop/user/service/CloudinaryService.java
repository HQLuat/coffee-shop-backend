package vn.edu.hcmuaf.fit.coffee_shop.user.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_FORMATS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final String FOLDER_NAME = "coffee-shop/avatars";

    /**
     * Upload avatar lên Cloudinary
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        validateFile(file);
        
        try {
            // Generate unique public_id
            String publicId = FOLDER_NAME + "/" + UUID.randomUUID().toString();
            
            // Upload với các options
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", FOLDER_NAME,
                            "resource_type", "image",
                            "transformation", new com.cloudinary.Transformation()
                                    .width(400).height(400)
                                    .crop("fill")
                                    .gravity("face")
                                    .quality("auto:good")
                    ));
            
            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("✅ Uploaded avatar successfully: {}", imageUrl);
            
            return imageUrl;
            
        } catch (IOException e) {
            log.error("❌ Error uploading avatar to Cloudinary: {}", e.getMessage());
            throw new IOException("Không thể upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Xóa avatar cũ từ Cloudinary
     */
    public void deleteAvatar(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        try {
            // Extract public_id from URL
            String publicId = extractPublicId(imageUrl);
            
            if (publicId != null && !publicId.isEmpty()) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("🗑️ Deleted old avatar: {} - Result: {}", publicId, result.get("result"));
            }
            
        } catch (Exception e) {
            log.error("❌ Error deleting avatar from Cloudinary: {}", e.getMessage());
            // Không throw exception vì việc xóa ảnh cũ fail không ảnh hưởng đến upload ảnh mới
        }
    }

    /**
     * Validate file upload
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File không được để trống");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Kích thước file không được vượt quá 5MB");
        }
        
        // Check file format
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("Tên file không hợp lệ");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        boolean isValidFormat = false;
        
        for (String format : ALLOWED_FORMATS) {
            if (format.equals(extension)) {
                isValidFormat = true;
                break;
            }
        }
        
        if (!isValidFormat) {
            throw new IOException("Chỉ chấp nhận file ảnh: JPG, JPEG, PNG, GIF, WEBP");
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("File phải là định dạng ảnh");
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     * Example: https://res.cloudinary.com/demo/image/upload/v1234567890/coffee-shop/avatars/abc123.jpg
     * -> coffee-shop/avatars/abc123
     */
    private String extractPublicId(String imageUrl) {
        try {
            if (!imageUrl.contains("cloudinary.com")) {
                return null;
            }
            
            // Tìm vị trí của "/upload/"
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }
            
            // Bỏ qua phần version (vXXXXXXXXXX)
            String afterUpload = imageUrl.substring(uploadIndex + 8);
            int slashIndex = afterUpload.indexOf("/");
            
            if (slashIndex != -1 && afterUpload.charAt(0) == 'v') {
                afterUpload = afterUpload.substring(slashIndex + 1);
            }
            
            // Loại bỏ extension (.jpg, .png, etc.)
            int lastDotIndex = afterUpload.lastIndexOf(".");
            if (lastDotIndex != -1) {
                afterUpload = afterUpload.substring(0, lastDotIndex);
            }
            
            return afterUpload;
            
        } catch (Exception e) {
            log.error("Error extracting public_id from URL: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}