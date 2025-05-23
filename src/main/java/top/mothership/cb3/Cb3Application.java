package top.mothership.cb3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Cb3Application {

    public static void main(String[] args) {
        SpringApplication.run(Cb3Application.class, args);
    }

}
