package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface AlfrescoPublicService {

    Mono<String> uploadFile(MultipartFile file , String filename);

    Mono<String> updateFile(String nodeId, MultipartFile file);

    Mono<ByteArrayResource> downloadFile(String nodeId );

    Mono<String> getDownloadUrl(String nodeId, String fileName);

    Mono<byte[]> downloadPdf(String nodeId);

     Mono<Void> deleteFile(String nodeId);

    String obtenerTokenAutenticacion();
}
