package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.rest;

import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.ArchivoResponse;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.UploadResponse;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.AlfrescoPrivateService;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.UtilArchivo;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/v2/t/almacenamiento/privado")
public class PrivateAlfrescoController {

    private final AlfrescoPrivateService alfrescoPrivateService;

    public PrivateAlfrescoController(AlfrescoPrivateService alfrescoPrivateService) {
        this.alfrescoPrivateService = alfrescoPrivateService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<UploadResponse>> subir(@RequestPart(value = "archivo") MultipartFile archivo,
                                                      @RequestParam(value = "carpetaId") String carpetaId) {

        log.info("getContentType " + archivo.getContentType());
        log.info("getOriginalFilename " +archivo.getOriginalFilename());
        log.info("getName " +archivo.getName());

        String extension = FilenameUtils.getExtension(archivo.getOriginalFilename());
        String filename = String.format("temp-%s.%s", UUID.randomUUID(), extension);

        log.info("filename " +filename);
        log.info("getOriginalFilename " + archivo.getOriginalFilename());

        return alfrescoPrivateService.uploadFile(archivo , carpetaId, filename)//archivo.getOriginalFilename()
               .map(nodeId -> ResponseEntity.ok(new UploadResponse.Builder(archivo, nodeId).build()));

    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<UploadResponse>> actualizar(@RequestPart(value = "archivo") MultipartFile archivo,
                                                           @RequestParam(value = "archivoId") String archivoId) {

        return alfrescoPrivateService.updateFile(archivoId, archivo)
                .map(nodeId -> ResponseEntity.ok(new UploadResponse.Builder(archivo, nodeId).build()));

    }

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<ByteArrayResource>> descargar(@RequestParam(value = "archivoId") String archivoId ,
                                                             @RequestParam(value = "nombreArchivo") String nombreArchivo) {

        String extension = FilenameUtils.getExtension(nombreArchivo);
        String contentType = UtilArchivo.getContentTypeByExtension(extension);


        return alfrescoPrivateService.downloadFile(archivoId)
                .map(resource -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file_downloaded\"")
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .contentLength(resource.contentLength())
                        .body(resource))
                .onErrorResume(WebClientResponseException.Unauthorized.class, e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)));
    }


    @GetMapping(path = "/visualizar",produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<ByteArrayResource>> visualizar(@RequestParam(value = "archivoId") String archivoId ,
                                                             @RequestParam(value = "nombreArchivo") String nombreArchivo) {

        String extension = FilenameUtils.getExtension(nombreArchivo);
        String contentType = UtilArchivo.getContentTypeByExtension(extension);


        return alfrescoPrivateService.downloadFile(archivoId)
                .map(resource -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"file_downloaded\"")
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .contentLength(resource.contentLength())
                        .body(resource))
                .onErrorResume(WebClientResponseException.Unauthorized.class, e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)));
    }


    @DeleteMapping()
    public Mono<ResponseEntity<Void>> eliminar(@RequestParam(value = "archivoId") String  archivoId) {

        return alfrescoPrivateService.deleteFile(archivoId)
                .map(e -> ResponseEntity.ok().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }


    @PostMapping("/mover")
    public ResponseEntity<ArchivoResponse> moverArchivo(
            @RequestParam String archivoId,
            @RequestParam String carpetaId) {

        Mono<ByteArrayResource> archivo =alfrescoPrivateService.downloadFile(archivoId);

        double peso = archivo.block().getByteArray().length/ 1048576.0;

        String hash = archivo
                .map(resource -> {
                    try {
                        byte[] archivoBytes = resource.getByteArray();
                        return UtilArchivo.calcularHash(archivoBytes);
                    } catch (Exception e) {
                        throw new RuntimeException("Error al obtener los bytes del archivo", e);
                    }
                }).block();

        alfrescoPrivateService.moverArchivo(archivoId, carpetaId).block();

        ArchivoResponse archivoResponse = new ArchivoResponse(archivoId, hash,peso);


        return ResponseEntity.ok(archivoResponse);
    }



}

