package one.epicsoft.vc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class VersionControlApplication {

  public static void main(final String[] args) {
    SpringApplication.run(VersionControlApplication.class, args);
  }
}
