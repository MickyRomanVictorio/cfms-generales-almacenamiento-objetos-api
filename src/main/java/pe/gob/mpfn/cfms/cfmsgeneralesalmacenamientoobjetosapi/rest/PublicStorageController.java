package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.rest;

import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.adapter.MinioAdapter;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.bean.Response;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.ResponseDto;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.payload.request.DownloadFileRequest;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.service.FileServiceApi;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.ArchivoUtil;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.Message;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/t/almacenamiento/publico")
@RequiredArgsConstructor
public class PublicStorageController {

    @Autowired
    private ArchivoUtil archivoUtil;

    @Autowired
    private final FileServiceApi fileServiceApi;

    @Autowired
    MinioAdapter minioAdapter;

    @PostMapping(value = "/uploadzip")
    public ResponseEntity<Object> uploadfiles(@RequestParam("file") List<MultipartFile> files) throws Exception {

        return ResponseEntity.ok().body(fileServiceApi.save(files));
    }

    @PostMapping(path = "/cargar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseDto> cargar(@RequestPart(value = "file", required = false) MultipartFile files, @RequestParam(value = "filename", required = false) String file) throws IOException {
        String extension = FilenameUtils.getExtension(files.getOriginalFilename());
        String filename = file != null ? file : String.format("temp-%s.%s", UUID.randomUUID(), extension);

        if(!archivoUtil.esTipoArchivoValido(files)) {
            return new ResponseEntity<>(new ResponseDto(null, Message.C_42201065_TIPOARCHIVO_INVALIDO), HttpStatus.BAD_REQUEST);
        }
        if(!archivoUtil.esArchivoValido(files)) {
            return new ResponseEntity<>(new ResponseDto(null, Message.C_42201064_ARCHIVO_INVALIDO), HttpStatus.BAD_REQUEST);
        }


        if (extension.equals("pdf")) {
            try {
                PdfReader pdfReader = new PdfReader(files.getBytes());
                PdfTextExtractor.getTextFromPage(pdfReader, 1);
                minioAdapter.uploadFile(filename, files.getBytes());
                return new ResponseEntity<>(new ResponseDto(new Response(filename, getPagesNumber(files.getBytes())), Message.ARCHIVO_SUBIDO), HttpStatus.OK);
            } catch (InvalidPdfException ex) {
                return new ResponseEntity<>(new ResponseDto(null, Message.ARCHIVO_CORRUPTO), HttpStatus.BAD_REQUEST);
            }
        } else {
            minioAdapter.uploadFile(filename, files.getBytes());
            return new ResponseEntity<>(new ResponseDto(new Response(filename), Message.ARCHIVO_SUBIDO), HttpStatus.OK);
        }
    }


    @PostMapping(path = "/validar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseDto> validarArchivo(@RequestPart(value = "file", required = false) MultipartFile file) {

        /*if(!archivoUtil.esTipoArchivoValido(file)) {
            return new ResponseEntity<>(new ResponseDto(null, Message.C_42201065_TIPOARCHIVO_INVALIDO), HttpStatus.BAD_REQUEST);
        }
        if(!archivoUtil.esArchivoValido(file)) {
            return new ResponseEntity<>(new ResponseDto(null, Message.C_42201064_ARCHIVO_INVALIDO), HttpStatus.BAD_REQUEST);
        }*/
        return new ResponseEntity<>(new ResponseDto(null, Message.ARCHIVO_VALIDO), HttpStatus.OK);
    }



    @GetMapping(path = "/descargar")
    public ResponseEntity<ByteArrayResource> descargar(@RequestParam(value = "filename") String file) throws IOException {
        byte[] data = minioAdapter.getFile(file);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + file + "\"")
                .body(resource);
    }



    @PostMapping(path = "/descargarAux")
    public ResponseEntity<ByteArrayResource> descargarAux(@RequestBody DownloadFileRequest file) throws IOException {
        byte[] data = minioAdapter.getFile(file.getFilename());
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + file.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping(path = "/eliminar")
    public ResponseEntity<ResponseDto> eliminar(@RequestParam(value = "filename") String file) {
        minioAdapter.deleteFile(file);

        ResponseDto responseDto = new ResponseDto(file, Message.ARCHIVO_ELIMINADO);
        return new ResponseEntity<>( responseDto, HttpStatus.OK);
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



}
