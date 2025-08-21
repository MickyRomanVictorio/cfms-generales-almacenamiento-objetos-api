package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface AlfrescoPrivateService {

    Mono<String> uploadFile(MultipartFile file, String carpetaId, String filename);

    Mono<String> updateFile(String nodeId, MultipartFile file);

    Mono<ByteArrayResource> downloadFile(String nodeId);

    Mono<Void> deleteFile(String nodeId);

    Mono<Void> moverArchivo(String fileNodeId, String targetFolderNodeId);
}
