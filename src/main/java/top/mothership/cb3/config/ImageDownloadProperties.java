package top.mothership.cb3.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片下载配置属性类
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "image.download")
public class ImageDownloadProperties {
    private String cachePath;
    private int maxCacheSize;
}