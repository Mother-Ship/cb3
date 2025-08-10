package top.mothership.cb3.command.image;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ClassPathImageCacheService {

    // 缓存已加载的Base64图片，避免重复读取
    private final Map<String, String> imageCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void preloadImages() {
        // 预加载常用图片
        try {
            preloadImage("lazer-result/images/lazer-a.svg");
            preloadImage("lazer-result/images/lazer-b.svg");
            preloadImage("lazer-result/images/lazer-banner-ctb.png");
            preloadImage("lazer-result/images/lazer-banner-mania.png");
            preloadImage("lazer-result/images/lazer-banner-std.png");
            preloadImage("lazer-result/images/lazer-banner-taiko.png");
            preloadImage("lazer-result/images/lazer-bg-ctb.svg");
            preloadImage("lazer-result/images/lazer-bg-mania.svg");
            preloadImage("lazer-result/images/lazer-bg-std.svg");
            preloadImage("lazer-result/images/lazer-bg-taiko.svg");
            preloadImage("lazer-result/images/lazer-c.svg");
            preloadImage("lazer-result/images/lazer-d.svg");
            preloadImage("lazer-result/images/lazer-f.svg");
            preloadImage("lazer-result/images/lazer-s.svg");
            preloadImage("lazer-result/images/lazer-x.svg");
        } catch (IOException e) {
            // 记录日志，但不影响应用启动
            log.warn("预加载图片失败: " + e.getMessage());
        }
    }

    private void preloadImage(String imagePath) throws IOException {
        getImageDataUrl(imagePath);
    }

    public String getImageAsBase64(String imagePath) throws IOException {

        try {
            ClassPathResource resource = new ClassPathResource("templates/" + imagePath);
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] imageBytes = StreamUtils.copyToByteArray(inputStream);
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法读取图片: " + imagePath, e);
        }

    }

    // 批量获取图片，用于一次性加载多个图片
    public Map<String, String> getImagesDataUrl(String... imagePaths) throws IOException {
        Map<String, String> images = new HashMap<>();
        for (String path : imagePaths) {
            images.put(path, getImageDataUrl(path));
        }
        return images;
    }

    public String getImageDataUrl(String imagePath) throws IOException {
        return imageCache.computeIfAbsent(imagePath + "_dataurl", key -> {
            try {
                String base64 = getImageAsBase64(imagePath);
                String mimeType = getMimeType(imagePath);
                return "data:" + mimeType + ";base64," + base64;
            } catch (IOException e) {
                throw new RuntimeException("无法生成图片DataURL: " + imagePath, e);
            }
        });
    }

    private String getMimeType(String imagePath) {
        String extension = imagePath.substring(imagePath.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/png";
        };
    }


}