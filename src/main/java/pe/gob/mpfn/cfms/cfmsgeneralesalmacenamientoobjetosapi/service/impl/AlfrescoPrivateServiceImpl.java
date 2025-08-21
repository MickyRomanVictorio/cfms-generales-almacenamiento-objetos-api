package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.config.AlfrescoConfig;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.AlfrescoPrivateService;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AlfrescoPrivateServiceImpl implements AlfrescoPrivateService {



    private final WebClient webClient;

    private final AlfrescoConfig alfrescoConfig;

    public AlfrescoPrivateServiceImpl(AlfrescoConfig alfrescoConfig) {

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
                                .build())
                .build();
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
    public Mono<String> uploadFile(MultipartFile file, String carpetaId, String filename) {
        return webClient.post()
                .uri("/nodes/{parentId}/children", carpetaId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("filedata", file.getResource()).with("name", filename)) //file.getOriginalFilename()
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseNodeIdFromResponse)
                .onErrorMap(e -> {
                    log.error("Fallo en la subida de Alfresco", e);
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
                .map(this::parseNodeIdFromResponse)
                .onErrorMap(e -> {
                    log.error("Failed to upload file to Alfresco", e);
                    return e;
                });
    }

    public Mono<ByteArrayResource> downloadFile(String nodeId) {

        return webClient.get()
                .uri("/nodes/{nodeId}/content", nodeId)
                .retrieve()
                .bodyToMono(DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new ByteArrayResource(bytes);
                })
                .doOnError(e -> log.error(e.getMessage(), nodeId, e));
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


    public Mono<Void> moverArchivo(String nodeId, String targetParentId) {
        return webClient.post()
                .uri("/nodes/{nodeId}/move", nodeId)  // Esta es la parte específica del endpoint
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{ \"targetParentId\": \"" + targetParentId + "\" }")
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> System.out.println("Error al mover archivo: " + e.getMessage()));


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




}