package top.mothership.cb3.command.image;

import io.github.headlesschrome.ChromiumLoader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import top.mothership.cb3.config.AppProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScreenShotService {

    private final AppProperties properties;

    private WebDriver driver;

    private Path fontDir;
    private Path jsDir;

    private Path cacheDirectory;

    @PostConstruct
    public void init() {
        val loader = new ChromiumLoader();

        val options = loader.downloadAndLoad();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--force-device-scale-factor=2");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.of(30, ChronoUnit.SECONDS));


        // 设置视口大小
        int width = 1920;
        int height = 1080;
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String windowSize = js.executeScript(
                "return (window.outerWidth - window.innerWidth + " + width + ") " +
                        "+ ',' + (window.outerHeight - window.innerHeight + " + height + "); ").toString();
        width = Integer.parseInt(windowSize.split(",")[0]);
        height = Integer.parseInt(windowSize.split(",")[1]);
        driver.manage().window().setSize(new Dimension(width, height));

        try {
            cacheDirectory = Paths.get(properties.getCachePath());
            Files.createDirectories(cacheDirectory);
            log.info("缓存目录初始化成功: {}", cacheDirectory.toAbsolutePath());
        } catch (IOException e) {
            log.error("创建缓存目录失败: {}", properties.getCachePath(), e);
            throw new RuntimeException("无法初始化缓存目录", e);
        }


        // 初始化时复制资源文件
        initResources();


        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (driver != null) {
                driver.quit();
            }
        }));
    }

    @SneakyThrows
    private void initResources() {
        // 创建临时目录和资源目录
        Path tempDir = cacheDirectory.resolve("result");

        // 创建字体和JS目录
        fontDir = tempDir.resolve("font");
        fontDir.toFile().mkdirs();
        jsDir = tempDir.resolve("js");
        jsDir.toFile().mkdirs();

        // 复制字体文件（仅当文件不存在时）
        Path torusRegular = fontDir.resolve("Torus-Regular.otf");
        if (!Files.exists(torusRegular)) {
            copyResource("/templates/lazer-result/font/Torus-Regular.otf", torusRegular);
        }

        Path torusLight = fontDir.resolve("Torus-Light.otf");
        if (!Files.exists(torusLight)) {
            copyResource("/templates/lazer-result/font/Torus-Light.otf", torusLight);
        }

        Path torusBold = fontDir.resolve("Torus-Bold.otf");
        if (!Files.exists(torusBold)) {
            copyResource("/templates/lazer-result/font/Torus-Bold.otf", torusBold);
        }

        // 复制JS文件（仅当文件不存在时）
        Path d3Min = jsDir.resolve("d3.min.js");
        if (!Files.exists(d3Min)) {
            copyResource("/templates/lazer-result/js/d3.min.js", d3Min);
        }
    }

    @SneakyThrows
    public byte[] htmlToImageWithLocalResources(String htmlContent) {

        initResources();
        Path tempDir = cacheDirectory.resolve("result");

        Path htmlFile = tempDir.resolve(UUID.randomUUID() + ".html");

        try {
            // 写入HTML文件
            Files.writeString(htmlFile, htmlContent);
            // 加载HTML
            driver.get("file://" + htmlFile.toAbsolutePath());
            // 截图
            TakesScreenshot screenshot = (TakesScreenshot) driver;

            return screenshot.getScreenshotAs(OutputType.BYTES);
        } finally {
            // 清理临时文件
            Files.delete(htmlFile);
        }
    }

    @SneakyThrows
    private void copyResource(String resourcePath, Path targetPath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, targetPath);
        }
    }
}
