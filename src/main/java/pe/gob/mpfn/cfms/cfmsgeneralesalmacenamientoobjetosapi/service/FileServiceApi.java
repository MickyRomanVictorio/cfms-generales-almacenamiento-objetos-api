package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service;

import org.springframework.web.multipart.MultipartFile;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.InfoResponse;
import java.util.List;

public interface FileServiceApi {

    public InfoResponse save(List<MultipartFile> files)throws Exception;

}
