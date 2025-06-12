package est.secretary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import est.secretary.configuration.ClientProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClientProperties.class)
@EnableCaching
public class SecretaryApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecretaryApplication.class, args);
	}

}
