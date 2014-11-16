package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.security.resource.EnableOAuth2Resource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@RestController
@EnableOAuth2Resource
public class Application {

    @RequestMapping("/hi/{name}")
    public String hi(@PathVariable String name) {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}


