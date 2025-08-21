package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${webclient.timeout}")
    private Integer timeout;
    @Value("${webclient.timeout-handler}")
    private Integer timeoutHandler;
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configura el HttpClient con timeouts y manejo de conexiones
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeoutHandler, TimeUnit.SECONDS)));

        // Devuelve el WebClient.Builder configurado
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}