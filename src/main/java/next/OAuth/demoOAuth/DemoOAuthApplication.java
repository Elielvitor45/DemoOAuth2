package next.OAuth.demoOAuth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoOAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoOAuthApplication.class, args);
		System.out.println("API Started");
	}

}
