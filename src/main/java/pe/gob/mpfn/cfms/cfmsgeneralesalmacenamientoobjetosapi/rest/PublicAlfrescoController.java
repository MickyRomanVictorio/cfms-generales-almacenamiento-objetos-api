package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.ResponseDto;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.UploadResponse;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.AlfrescoPublicService;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.*;
import reactor.core.publisher.Mono;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
@RestController
@RequestMapping("/v2/t/almacenamiento/publico")
public class PublicAlfrescoController {

    private final AlfrescoPublicService alfrescoService;

    public PublicAlfrescoController(AlfrescoPublicService alfrescoService) {
        this.alfrescoService = alfrescoService;
    }

    private static final Map<String, String> mimeTypes = new HashMap<>();

    static {
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("pdf", "application/pdf");
        // Agrega más tipos MIME según sea necesario
    }

    // Clase para mantener el estado del procesamiento del archivo
    private static class ProcessedFile {
        final byte[] bytes;
        final String extension;
        final String contentType;
        final int numeroPaginas;

        ProcessedFile(byte[] bytes, String extension, String contentType, int numeroPaginas) {
            this.bytes = bytes;
            this.extension = extension;
            this.contentType = contentType;
            this.numeroPaginas = numeroPaginas;
        }
    }

    private ProcessedFile processFile(byte[] fileBytes, String originalExtension, String contentType) {
        log.info("Procesando archivo - Tamaño original: {} bytes ({} MB)",
                fileBytes.length, bytesToMegabytes(fileBytes.length));

        try {
            // Intentar descomprimir el archivo
            byte[] decompressedData = SevenZ.descomprimir(fileBytes);
            if (decompressedData != null && decompressedData.length > 0) {
                log.info("Archivo descomprimido exitosamente - Tamaño descomprimido: {} bytes ({} MB)",
                        decompressedData.length, bytesToMegabytes(decompressedData.length));
                // Si se pudo descomprimir, asumimos que es un PDF
                return new ProcessedFile(
                        decompressedData,
                        "pdf",
                        "application/pdf",
                        getPagesNumber(decompressedData)
                );
            }
        } catch (Exception e) {
            log.info("El archivo no es un ZIP/7Z o no se pudo descomprimir: " + e.getMessage());
        }

        log.info("Procesando archivo como archivo normal - Tamaño: {} bytes ({} MB)",
                fileBytes.length, bytesToMegabytes(fileBytes.length));
        // Si no se pudo descomprimir o no es un ZIP/7Z, procesamos como archivo normal
        return new ProcessedFile(
                fileBytes,
                originalExtension,
                contentType,
                originalExtension.equals("pdf") ? getPagesNumber(fileBytes) : 1
        );
    }

    @PostMapping(value = "/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ResponseDto>> cargar(
            @RequestPart(value = "file", required = false) MultipartFile file, @RequestParam(value = "nodeId", required = false) String idArchivo) throws IOException {

        log.info("getContentType " + file.getContentType());
        log.info("getOriginalFilename " + file.getOriginalFilename());
        log.info("getSize " + file.getSize());
        try {

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String filename = String.format("temp-%s.%s", UUID.randomUUID(), extension);
            String contentType = file.getContentType();
            double pesoArchivoMb = bytesToMegabytes(file.getSize());
            Integer numeroPaginas = extension.equals("pdf") ? getPagesNumber(file.getBytes()) : 1;
            String hash = UtilArchivo.calcularHash(file.getBytes());


            if(idArchivo != null){

                return alfrescoService.updateFile(idArchivo, file)
                        .flatMap(response -> alfrescoService.getDownloadUrl(idArchivo,filename)
                                .map(url -> new ResponseDto(new UploadResponse(idArchivo,filename,contentType,extension,numeroPaginas,pesoArchivoMb), Message.ARCHIVO_SUBIDO)))
                        .map(responseDto -> ResponseEntity.ok(responseDto))
                        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO))));

            }else{

                log.info("uploadFile INICIO");
                return alfrescoService.uploadFile(file , filename)
                        .flatMap(nodeId -> alfrescoService.getDownloadUrl(nodeId, file.getOriginalFilename()) //file.getOriginalFilename()
                                .map(url -> new ResponseDto(new UploadResponse(nodeId,filename,contentType,extension,numeroPaginas,pesoArchivoMb), Message.ARCHIVO_SUBIDO)))
                        .map(responseDto -> ResponseEntity.ok(responseDto))
                        .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO))));

            }


        } catch (WebClientResponseException e) {
            log.info("WebClientResponseException");
            return Mono.just(ResponseEntity.status(e.getRawStatusCode()).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)));
        } catch (Exception e) {
            log.info("Exception: " + e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)
                    ));
        }

    }

    @PostMapping(value = "/cargar-comprimido", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ResponseDto>> cargarComprimido(
            @RequestPart(value = "file", required = false) MultipartFile file, @RequestParam(value = "nodeId", required = false) String idArchivo) throws IOException {

        log.info("Iniciando carga de archivo - Nombre: {}, Tipo reportado: {}, Tamaño: {} bytes ({} MB)",
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                bytesToMegabytes(file.getSize()));

        try {
            byte[] fileBytes = file.getBytes();

            // Validar el tipo MIME real del archivo
            String realMimeType = Files.probeContentType(Paths.get(file.getOriginalFilename()));
            if (realMimeType == null) {
                // Si no se puede determinar por el nombre, intentar con los bytes
                try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                    realMimeType = URLConnection.guessContentTypeFromStream(is);
                }
            }

            log.info("Tipo MIME real detectado: {}", realMimeType);

            // Validar que el tipo MIME reportado coincida con el real
            if (realMimeType != null && !realMimeType.equals(file.getContentType())) {
                log.warn("Inconsistencia en tipo MIME - Reportado: {}, Real: {}", file.getContentType(), realMimeType);
            }

            log.info("Archivo leído en memoria - Tamaño: {} bytes ({} MB)",
                    fileBytes.length, bytesToMegabytes(fileBytes.length));

            String originalExtension = FilenameUtils.getExtension(file.getOriginalFilename());
            String contentType = file.getContentType();
            double pesoArchivoMb = bytesToMegabytes(file.getSize());
            String hash = UtilArchivo.calcularHash(fileBytes);

            byte[] decompressedData = unzipFile(file.getBytes());

            int totalPaginas = getPagesNumber(decompressedData);

            // Procesar el archivo
            final ProcessedFile processedFile = processFile(fileBytes, originalExtension, contentType);
            log.info("Archivo procesado - Tamaño final: {} bytes ({} MB), Extensión: {}, Tipo: {}",
                    processedFile.bytes.length,
                    bytesToMegabytes(processedFile.bytes.length),
                    processedFile.extension,
                    processedFile.contentType);

            String filename = String.format("temp-%s.%s", UUID.randomUUID(), processedFile.extension);
            MultipartFile multipartFile = new ByteArrayMultipartFile(processedFile.bytes, filename, processedFile.contentType);
            log.info("Archivo listo para subir a Alfresco - Nombre: {}, Tamaño: {} bytes ({} MB)",
                    filename,
                    multipartFile.getSize(),
                    bytesToMegabytes(multipartFile.getSize()));

            if(idArchivo != null) {
                log.info("Actualizando archivo existente - NodeId: {}", idArchivo);
                return alfrescoService.updateFile(idArchivo, multipartFile)
                        .flatMap(response -> {
                            log.info("Archivo actualizado en Alfresco - NodeId: {}", idArchivo);
                            return alfrescoService.getDownloadUrl(idArchivo, filename)
                                    .map(url -> new ResponseDto(new UploadResponse(idArchivo, filename, processedFile.contentType, processedFile.extension, totalPaginas, pesoArchivoMb), Message.ARCHIVO_SUBIDO));
                        })
                        .map(responseDto -> ResponseEntity.ok(responseDto))
                        .onErrorResume(e -> {
                            log.error("Error al actualizar archivo en Alfresco: {}", e.getMessage(), e);
                            return Mono.just(ResponseEntity.status(500).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)));
                        });
            } else {
                log.info("Subiendo nuevo archivo a Alfresco");
                return alfrescoService.uploadFile(multipartFile, filename)
                        .flatMap(nodeId -> {
                            log.info("Archivo subido exitosamente a Alfresco - NodeId: {}", nodeId);
                            return alfrescoService.getDownloadUrl(nodeId, filename)
                                    .map(url -> new ResponseDto(new UploadResponse(nodeId, filename, processedFile.contentType, processedFile.extension, totalPaginas, pesoArchivoMb), Message.ARCHIVO_SUBIDO));
                        })
                        .map(responseDto -> ResponseEntity.ok(responseDto))
                        .onErrorResume(e -> {
                            log.error("Error al subir archivo a Alfresco: {}", e.getMessage(), e);
                            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)));
                        });
            }

        } catch (WebClientResponseException e) {
            log.error("Error de respuesta de Alfresco - Status: {}, Mensaje: {}", e.getRawStatusCode(), e.getMessage(), e);
            return Mono.just(ResponseEntity.status(e.getRawStatusCode()).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)));
        } catch (Exception e) {
            log.error("Error inesperado al procesar archivo: {}", e.getMessage(), e);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)));
        }
    }

    @PostMapping(value = "/upload-chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ResponseDto>> uploadChunk(
            @RequestPart("chunk") MultipartFile chunkPart,
            @RequestParam("fileId") String fileId,
            @RequestParam("index") Integer index,
            @RequestParam("total") Integer total,
            @RequestParam("fileName") String fileName) {

        Path uploadDir = Paths.get(System.getProperty("java.io.tmpdir"), fileId);
        try {
            Files.createDirectories(uploadDir);

            Path chunkFile = uploadDir.resolve("chunk_" + index);
            chunkPart.transferTo(chunkFile.toFile());

            if (index.equals(total - 1)) {
                mergeChunks(uploadDir, fileName, total);

                File fileTerminado = uploadDir.resolve(fileName).toFile();
                String contentType = Files.probeContentType(uploadDir.resolve(fileName));
                MultipartFile multipartFile = new FileSystemMultipartFile(fileTerminado, contentType);

                String extension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
                Integer numeroPaginas = extension.equals("pdf") ? getPagesNumber(multipartFile.getBytes()) : 1;
                double pesoArchivoMb = bytesToMegabytes(multipartFile.getSize());

                log.info("uploadFile INICIO");
                log.info("getOriginalFilename " + chunkPart.getOriginalFilename());
                log.info("getSize " + multipartFile.getSize());
                return alfrescoService.uploadFile(multipartFile , fileName)
                        .flatMap(nodeId -> alfrescoService.getDownloadUrl(nodeId, fileName) //file.getOriginalFilename()
                                .map(url -> new ResponseDto(new UploadResponse(nodeId,fileName,contentType,extension,numeroPaginas,pesoArchivoMb), Message.ARCHIVO_SUBIDO)))
                        .map(responseDto -> ResponseEntity.ok(responseDto))
                        .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO))));

                //return ResponseEntity.ok("Archivo completo: " + fileName);
            }
            return Mono.just(ResponseEntity.ok(new ResponseDto("Chunk " + index + " recibido", Message.ARCHIVO_SUBIDO)));
        } catch (WebClientResponseException e) {
            log.info("WebClientResponseException");
            return Mono.just(ResponseEntity.status(e.getRawStatusCode()).body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)));
        } catch (Exception e) {
            log.info("Exception: " + e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseDto(null, Message.ERROR_SUBIDA_ARCHIVO)
                    ));
        }
    }

    private void mergeChunks(Path uploadDir, String fileName, int totalChunks) throws IOException {
        Path mergedFile = uploadDir.resolve(fileName);
        try (OutputStream os = Files.newOutputStream(mergedFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunk = uploadDir.resolve("chunk_" + i);
                Files.copy(chunk, os);
                Files.delete(chunk);
            }
        }
    }

    @GetMapping(value = "/descargar", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<ByteArrayResource>> downloadFile(@RequestParam(value = "nodeId") String nodeId) {
        log.info("Iniciando descarga de archivo - NodeId: {}", nodeId);

        return alfrescoService.downloadFile(nodeId)
                .map(resource -> {
                    log.info("Archivo recibido de Alfresco - Tamaño: {} bytes ({} MB)",
                            resource.contentLength(),
                            bytesToMegabytes(resource.contentLength()));

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file_downloaded\"")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                            .contentLength(resource.contentLength())
                            .body(resource);
                })
                .doOnError(e -> log.error("Error al descargar archivo de Alfresco: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(null)));
    }

    @GetMapping(value = "/descargar-descomprimido", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<ByteArrayResource>> downloadAndDecompressFile(@RequestParam(value = "nodeId") String nodeId) {
        log.info("Iniciando descarga y descompresión de archivo - NodeId: {}", nodeId);

        return alfrescoService.downloadFile(nodeId)
                .map(resource -> {
                    try {
                        byte[] compressedData = resource.getByteArray();
                        log.info("Archivo recibido de Alfresco - Tamaño: {} bytes ({} MB)",
                                compressedData.length,
                                bytesToMegabytes(compressedData.length));

                        if (isZipFile(compressedData)) {
                            log.info("Archivo detectado como ZIP - Intentando descomprimir con ZIP");
                            byte[] decompressedData = unzipFile(compressedData);
                            if (decompressedData != null) {
                                log.info("Archivo descomprimido exitosamente con ZIP - Tamaño: {} bytes ({} MB)",
                                        decompressedData.length,
                                        bytesToMegabytes(decompressedData.length));

                                ByteArrayResource decompressedResource = new ByteArrayResource(decompressedData);
                                String contentType = determineContentType(nodeId);

                                return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archivo_descomprimido\"")
                                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                                        .contentLength(decompressedData.length)
                                        .body(decompressedResource);
                            } else {
                                log.warn("Fallo la descompresión ZIP - Se devolverá el archivo original");
                            }
                        } else {
                            log.info("El archivo no es un ZIP - Se devolverá el archivo original");
                        }
                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"archivo_original\"")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                .contentLength(compressedData.length)
                                .body(resource);

                    } catch (Exception e) {
                        log.error("Error al procesar el archivo: {}", e.getMessage(), e);
                        throw new RuntimeException("Error al procesar el archivo", e);
                    }
                })
                .doOnError(e -> log.error("Error al procesar archivo: {}", e.getMessage(), e))
                .onErrorResume(e -> {
                    log.error("Error al descargar o descomprimir archivo: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
                });
    }

    private boolean isZipFile(byte[] data) {
        if (data.length < 4) return false;
        // Verificar la firma ZIP (PK\x03\x04)
        return (data[0] == 0x50 && data[1] == 0x4B && data[2] == 0x03 && data[3] == 0x04);
    }

    private byte[] unzipFile(byte[] zipData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Tomar el primer archivo del ZIP
            if ((entry = zis.getNextEntry()) != null) {
                log.info("Descomprimiendo archivo ZIP: {}", entry.getName());

                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }

                zis.closeEntry();
                return outputStream.toByteArray();
            }

            log.warn("El archivo ZIP está vacío");
            return null;
        } catch (Exception e) {
            log.error("Error al descomprimir archivo ZIP: {}", e.getMessage(), e);
            return null;
        }
    }

    private String determineContentType(String nodeId) {
        // Aquí podrías implementar la lógica para determinar el tipo de contenido
        // basado en la extensión del archivo o el nodeId
        // Por ahora retornamos un tipo genérico
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    @PutMapping(value = "/actualizar/{nodeId}")
    public Mono<ResponseEntity<ResponseDto>> updateFile(@PathVariable String nodeId, @RequestPart("file") MultipartFile file) throws IOException {

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        double pesoArchivoMb = bytesToMegabytes(file.getSize());
        Integer numeroPaginas = extension.equals("pdf") ? getPagesNumber(file.getBytes()) : 1;

        return alfrescoService.updateFile(nodeId, file)
                .flatMap(response -> alfrescoService.getDownloadUrl(nodeId, file.getOriginalFilename())
                        .map(url -> new ResponseDto(new UploadResponse( nodeId,filename,contentType,extension,numeroPaginas,pesoArchivoMb), Message.ARCHIVO_SUBIDO)))
                .map(responseDto -> ResponseEntity.ok(responseDto))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(new ResponseDto(null, Message.ARCHIVO_CORRUPTO))));
    }


    @DeleteMapping(value = "/eliminar")
    public Mono<ResponseEntity<ResponseDto>> eliminarArchivo(@RequestParam(value = "nodeId") String  nodeId) {

        return alfrescoService.deleteFile(nodeId)
                .then(Mono.just(ResponseEntity.ok(new ResponseDto(null, Message.ARCHIVO_ELIMINADO))))
                .onErrorResume(e -> {
                    e.printStackTrace(); // Considera usar un logger en lugar de printStackTrace
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ResponseDto(null, Message.ERROR_ELIMINAR_ARCHIVO)));
                });
    }


    @PostMapping("/token")
    public ResponseEntity<String> obtenerToken(@RequestParam String usuario, @RequestParam String password) {
        try {
            String token = alfrescoService.obtenerTokenAutenticacion();
            return ResponseEntity.ok(token);
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }



    private int getPagesNumber(byte[] file) {
        int pageCount = 0;
        try {
            PDDocument pdfDoc = Loader.loadPDF(file);
            pageCount = pdfDoc.getNumberOfPages();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return pageCount;
    }

    public MultipartFile zipearArchivo(List<MultipartFile> files ) throws Exception {
        String originalFilename= String.format("temp-%s.%s", UUID.randomUUID(), "7z");
        byte[] zipped = SevenZ.comprimir(files);
        return new ByteArrayMultipartFile(zipped, originalFilename, "application/x-7z-compressed");

    }

    public double bytesToMegabytes(long bytes) {
        return bytes / 1048576.0;
    }



}
