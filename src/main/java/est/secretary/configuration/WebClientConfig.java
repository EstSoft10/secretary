package est.secretary.configuration;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient webClient() {
		TcpClient tcpClient = TcpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
			.doOnConnected(conn ->
				conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
					.addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS))
			);

		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
			.build();
	}
}
