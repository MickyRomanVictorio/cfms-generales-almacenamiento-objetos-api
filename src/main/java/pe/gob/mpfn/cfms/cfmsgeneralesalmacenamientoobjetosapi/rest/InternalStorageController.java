package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.rest;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.adapter.MinioAdapter;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.ResponseDto;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.Message;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/v1/t/almacenamiento/interno")
public class InternalStorageController {
    @Autowired
    MinioAdapter minioAdapter;

    @PostMapping(path = "/cargar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseDto> cargar(@RequestPart(value = "file", required = false) MultipartFile files, @RequestParam(value = "filename", required = false) String file, @RequestParam(value = "bucket") String bucket, @RequestParam(value = "path") String path) throws IOException {
        String extension = FilenameUtils.getExtension(files.getOriginalFilename());
        String filename = file != null ? file : String.format("temp-%s.%s", UUID.randomUUID(), extension);
        minioAdapter.uploadFileCustomLocation(bucket, path, filename, files.getBytes());

        ResponseDto responseDto = new ResponseDto(filename, Message.ARCHIVO_SUBIDO);
        return new ResponseEntity<>( responseDto, HttpStatus.OK);
    }

    @GetMapping(path = "/descargar")
    public ResponseEntity<ByteArrayResource> descargar(@RequestParam(value = "filename") String file, @RequestParam(value = "bucket") String bucket, @RequestParam(value = "path") String path) throws IOException {
        byte[] data = minioAdapter.getFileCustomLocation(bucket, path, file);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + file + "\"")
                .body(resource);
    }

    @DeleteMapping(path = "/eliminar")
    public ResponseEntity<ResponseDto> eliminar(@RequestParam(value = "filename") String file, @RequestParam(value = "bucket") String bucket, @RequestParam(value = "path") String path) {
        minioAdapter.deleteFile(file);

        ResponseDto responseDto = new ResponseDto(file, Message.ARCHIVO_ELIMINADO);
        return new ResponseEntity<>( responseDto, HttpStatus.OK);
    }
}
