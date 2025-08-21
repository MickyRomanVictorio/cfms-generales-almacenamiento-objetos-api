package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.adapter.MinioAdapter;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.InfoResponse;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.FileServiceApi;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.SevenZ;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class FIleServiceImpl implements FileServiceApi {

    private static final String OUTPUT_DIRECTORY = "/tmp";

    @Autowired
    MinioAdapter minioAdapter;


    @Override
    public InfoResponse save(List<MultipartFile> files) throws Exception {

        InfoResponse rpta = new InfoResponse();

        String filename= String.format("temp-%s.%s", UUID.randomUUID(), "7z");
        byte[] zipped = SevenZ.comprimir(files);
        minioAdapter.uploadFile(filename, zipped);
        rpta.setNombrezip(filename);
        rpta.setMessage("se subio el archivo al Minio");
        rpta.setCode(200);
        return rpta;

    }

}
