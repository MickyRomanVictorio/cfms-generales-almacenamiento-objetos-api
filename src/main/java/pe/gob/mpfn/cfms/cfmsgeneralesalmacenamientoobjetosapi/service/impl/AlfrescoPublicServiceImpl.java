package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.config.AlfrescoConfig;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.AlfrescoPublicService;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class AlfrescoPublicServiceImpl implements AlfrescoPublicService {
    @Value("${nodes.temp.id}")
    private String nodeId;

    private final WebClient webClient;

    private final AlfrescoConfig alfrescoConfig;

    public AlfrescoPublicServiceImpl(AlfrescoConfig alfrescoConfig) {

        int bufferSizeInBytes = parseBufferSize(alfrescoConfig.getmaxInMemorySize());

        this.alfrescoConfig = alfrescoConfig;
        HttpClient httpClient = HttpClient.create();

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        this.webClient = WebClient.builder()
                .clientConnector(connector)
                .baseUrl(alfrescoConfig.getUrlBase())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((alfrescoConfig.getUsername() + ":" + alfrescoConfig.getPassword()).getBytes()))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSizeInBytes)) // Set max in-memory size to 10 MB
                                .build()).build();

    }

    private int parseBufferSize(String size) {

        size = size.toUpperCase();
        if (size.endsWith("MB")) {
            return Integer.parseInt(size.replace("MB", "").trim()) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Integer.parseInt(size.replace("KB", "").trim()) * 1024;
        } else {
            return Integer.parseInt(size);
        }
    }


    @Override
    public Mono<String> uploadFile(MultipartFile file , String filename) {
        return webClient.post()
                .uri("/nodes/{parentId}/children", nodeId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("filedata", file.getResource()).with("name", filename)) //file.getOriginalFilename()
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .map(this::parseNodeIdFromResponse)
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("HTTP error while uploading file to Alfresco: " + ex.getStatusCode(), ex);
                    return ex;
                })
                .onErrorMap(e -> {
                    log.error("Failed to upload file to Alfresco", e);
                    return e;
                });
    }

    @Override
    public Mono<String> updateFile(String nodeId, MultipartFile file) {
        return webClient.put()
                .uri("/nodes/{nodeId}/content", nodeId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromResource(file.getResource()))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorMap(e -> {
                    log.error("Failed to upload file to Alfresco", e);
                    return e;
                });
    }


    public Mono<ByteArrayResource> downloadFile(String nodeId  ) {

        return webClient.get()
                .uri("/nodes/{nodeId}/content", nodeId)

                .retrieve()
                .bodyToMono(DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);  // Leer los bytes del buffer
                    DataBufferUtils.release(dataBuffer);  // Liberar el buffer
                    return new ByteArrayResource(bytes);  // Envolver el array de bytes en ByteArrayResource
                })
                .doOnError(e -> log.error(e.getMessage(), nodeId, e));
    }


    @Override
    public Mono<String> getDownloadUrl(String nodeId, String fileName) {
        // String baseUrl = "http://10.40.121.102:8080/share/proxy/alfresco/slingshot/node/content/workspace/SpacesStore/";
        /// return Mono.just(baseUrl + nodeId + "/" + fileName);

        return Mono.just(alfrescoConfig.getUrlShare() + nodeId + "/" + fileName);
    }

    private String parseNodeIdFromResponse(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("entry").path("id").asText();
        } catch (Exception e) {
            log.error("Failed to parse Alfresco response", e);
            return null;
        }
    }


    @Override
    public Mono<byte[]> downloadPdf(String nodeId) {
        return webClient.get()
                .uri("/nodes/" + nodeId + "/content")
                .retrieve()
                .bodyToMono(DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });
    }


    @Override
    public Mono<Void> deleteFile(String nodeId) {

        String deleteUrl = String.format("%s/nodes/%s", alfrescoConfig.getUrlBase(), nodeId);

        return webClient.delete()
                .uri(deleteUrl)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(e -> {
                    log.error("Failed to delete file from Alfresco", e);
                    return e;
                });
    }

    @Override
    public String obtenerTokenAutenticacion() {

        return webClient.post()
                .uri( "http://10.40.121.102:8080/alfresco/api/-default-/public/authentication/versions/1/tickets")
                .bodyValue(Map.of("userId", alfrescoConfig.getUsername(), "password", alfrescoConfig.getPassword()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.path("entry").path("id").asText())
                .block();
    }



}