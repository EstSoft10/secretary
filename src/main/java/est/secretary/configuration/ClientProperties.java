package est.secretary.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "client")
@Getter
@Setter
public class ClientProperties {
	private String id;
	private List<String> ids;
}
