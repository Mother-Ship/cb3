package top.mothership.cb3.command.image;

import io.github.headlesschrome.ChromiumLoader;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.val;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ScreenShotService {

    private WebDriver driver;

    @PostConstruct
    public void init() {
//        WebDriverManager.chromedriver().setup();
        val loader = new ChromiumLoader();

        val options = loader.downloadAndLoad();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.of(30, ChronoUnit.SECONDS));

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (driver != null) {
                driver.quit();
            }
        }));
    }

    @SneakyThrows
    public byte[] htmlToImageWithLocalResources(String htmlContent) {
        // 确保HTML中的本地资源能正确加载
        Path tempDir = Files.createTempDirectory("result");
        Path htmlFile = tempDir.resolve(UUID.randomUUID() + ".html");

        try {
            // 写入HTML文件
            Files.writeString(htmlFile, htmlContent);
            // 加载HTML
            driver.get("file://" + htmlFile.toAbsolutePath());
            // 等待所有资源加载完成
            Thread.sleep(3000);
            // 截图
            TakesScreenshot screenshot = (TakesScreenshot) driver;

            return screenshot.getScreenshotAs(OutputType.BYTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("截图过程被中断", e);
        } finally {
            // 清理临时文件
            Files.delete(htmlFile);
        }
    }
}