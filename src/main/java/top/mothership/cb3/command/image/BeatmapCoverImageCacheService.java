package top.mothership.cb3.command.image;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.mothership.cb3.config.AppProperties;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


@Component
@RequiredArgsConstructor
@Slf4j
public class BeatmapCoverImageCacheService {
    private static final String IMAGE_EXTENSION = ".jpg";

    private final AppProperties properties;

    private final ConcurrentHashMap<String, ReentrantLock> downloadLocks = new ConcurrentHashMap<>();
    private Path cacheDirectory;

    @PostConstruct
    public void init() {
        try {
            cacheDirectory = Paths.get(properties.getCachePath() + "/beatmap_covers");
            Files.createDirectories(cacheDirectory);
            log.info("图片缓存目录初始化成功: {}", cacheDirectory.toAbsolutePath());
        } catch (IOException e) {
            log.error("创建缓存目录失败: {}", properties.getCachePath(), e);
            throw new RuntimeException("无法初始化图片缓存目录", e);
        }
    }

    /**
     * 获取beatmapset的封面图片
     *
     * @param beatmapsetId beatmapset ID
     * @return 图片文件路径，如果下载失败返回null
     */
    public String getImage(String beatmapsetId) {
        if (!StringUtils.hasText(beatmapsetId)) {
            log.warn("beatmapsetId不能为空");
            return null;
        }

        String fileName = beatmapsetId + IMAGE_EXTENSION;
        Path imagePath = cacheDirectory.resolve(fileName);

        // 如果文件已存在，直接返回
        if (Files.exists(imagePath)) {
            log.debug("从缓存获取图片: {}", imagePath);
            return imagePath.toString();
        }

        // 使用锁确保同一个ID只下载一次
        ReentrantLock lock = downloadLocks.computeIfAbsent(beatmapsetId, k -> new ReentrantLock());
        lock.lock();

        try {
            // 再次检查文件是否存在（双重检查）
            if (Files.exists(imagePath)) {
                log.debug("从缓存获取图片（双重检查）: {}", imagePath);
                return imagePath.toString();
            }

            // 下载图片
            if (downloadImage(beatmapsetId, imagePath)) {
                log.info("成功下载并缓存图片: {} -> {}", beatmapsetId, imagePath);
                return imagePath.toString();
            } else {
                log.warn("下载图片失败: {}", beatmapsetId);
                return null;
            }
        } finally {
            lock.unlock();
            downloadLocks.remove(beatmapsetId);
        }
    }

    /**
     * 下载图片到指定路径
     *
     * @param beatmapsetId beatmapset ID
     * @param targetPath   目标文件路径
     * @return 是否下载成功
     */
    private boolean downloadImage(String beatmapsetId, Path targetPath) {
        String imageUrl = String.format("%s/%s/covers/fullsize.jpg", "https://assets.ppy.sh/beatmaps", beatmapsetId);

        try {
            log.debug("开始下载图片: {} -> {}", imageUrl, targetPath);

            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置连接参数
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "BeatmapImageCache/1.0");

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("下载图片失败，HTTP响应码: {} for {}", responseCode, imageUrl);
                return false;
            }

            // 获取文件大小
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0) {
                log.debug("图片大小: {} bytes", contentLength);
            }

            // 下载文件
            try (InputStream inputStream = connection.getInputStream();
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {

                // 先下载到临时文件，然后原子性移动
                Path tempFile = targetPath.getParent().resolve(targetPath.getFileName() + ".tmp");
                Files.copy(bufferedInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

                return true;
            }

        } catch (Exception e) {
            log.error("下载图片异常: {} -> {}", imageUrl, targetPath, e);

            // 清理可能存在的不完整文件
            try {
                Files.deleteIfExists(targetPath);
                Files.deleteIfExists(targetPath.getParent().resolve(targetPath.getFileName() + ".tmp"));
            } catch (IOException cleanupException) {
                log.warn("清理临时文件失败", cleanupException);
            }

            return false;
        }
    }

    /**
     * 清理缓存中的指定图片
     *
     * @param beatmapsetId beatmapset ID
     * @return 是否清理成功
     */
    public boolean clearCache(String beatmapsetId) {
        if (!StringUtils.hasText(beatmapsetId)) {
            return false;
        }

        String fileName = beatmapsetId + IMAGE_EXTENSION;
        Path imagePath = cacheDirectory.resolve(fileName);

        try {
            boolean deleted = Files.deleteIfExists(imagePath);
            if (deleted) {
                log.info("清理缓存图片成功: {}", imagePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("清理缓存图片失败: {}", imagePath, e);
            return false;
        }
    }

    /**
     * 获取缓存目录中的文件数量
     *
     * @return 缓存文件数量
     */
    public long getCacheFileCount() {
        try {
            return Files.list(cacheDirectory)
                    .filter(path -> path.toString().endsWith(IMAGE_EXTENSION))
                    .count();
        } catch (IOException e) {
            log.error("统计缓存文件数量失败", e);
            return 0;
        }
    }

    /**
     * 检查图片是否已缓存
     *
     * @param beatmapsetId beatmapset ID
     * @return 是否已缓存
     */
    public boolean isCached(String beatmapsetId) {
        if (!StringUtils.hasText(beatmapsetId)) {
            return false;
        }

        String fileName = beatmapsetId + IMAGE_EXTENSION;
        Path imagePath = cacheDirectory.resolve(fileName);
        return Files.exists(imagePath);
    }
}
