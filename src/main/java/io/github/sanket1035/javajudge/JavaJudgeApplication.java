package io.github.sanket1035.javajudge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JavaJudgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(JavaJudgeApplication.class, args);
    }
}
