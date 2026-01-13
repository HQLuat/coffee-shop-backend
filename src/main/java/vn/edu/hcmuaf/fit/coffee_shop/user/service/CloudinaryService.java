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
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOWED_FORMATS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final String FOLDER_NAME = "coffee-shop/avatars";
    private static final String FOLDER_PRODUCT = "coffee-shop/products";

    public String uploadAvatar(MultipartFile file) throws IOException {
        validateFile(file);
        
        try {
            // Generate unique public_id
            String publicId = FOLDER_NAME + "/" + UUID.randomUUID().toString();
            
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
            
            return imageUrl;
            
        } catch (IOException e) {
            log.error("Error uploading avatar to Cloudinary: {}", e.getMessage());
            throw new IOException("Không thể upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    public void deleteAvatar(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        try {
            // Extract public_id from URL
            String publicId = extractPublicId(imageUrl);
            
            if (publicId != null && !publicId.isEmpty()) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
            
        } catch (Exception e) {
            log.error("Error deleting avatar from Cloudinary: {}", e.getMessage());
        }
    }

    public String uploadProductImage(MultipartFile file) throws IOException {
        validateFile(file); 
        
        try {
            String publicId = FOLDER_PRODUCT + "/" + UUID.randomUUID().toString();
            
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", FOLDER_PRODUCT,
                            "resource_type", "image",
                            "transformation", new com.cloudinary.Transformation()
                                    .width(800).height(800)
                                    .crop("fill")
                                    .quality("auto:good")
                    ));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Error uploading product image: {}", e.getMessage());
            throw new IOException("Không thể upload ảnh sản phẩm: " + e.getMessage());
        }
    }

    public void deleteProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        try {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null && !publicId.isEmpty()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            log.error("Error deleting product image: {}", e.getMessage());
        }
    }

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

    private String extractPublicId(String imageUrl) {
        try {
            if (!imageUrl.contains("cloudinary.com")) {
                return null;
            }
            
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }
            
            String afterUpload = imageUrl.substring(uploadIndex + 8);
            int slashIndex = afterUpload.indexOf("/");
            
            if (slashIndex != -1 && afterUpload.charAt(0) == 'v') {
                afterUpload = afterUpload.substring(slashIndex + 1);
            }
            
            // Remove extension (.jpg, .png, etc.)
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

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}