package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto;

import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.Constante;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.UtilArchivo;

@Data
@Builder
public  class UploadResponse {

    private String nodeId;
    private String filename;
    private String contentType;
    private String extension;
    private Integer numeroPaginas;
    private double pesoArchivosMb;

    public UploadResponse(
            String nodeId,String filename,
            String contentType,  String extension,
            Integer numeroPaginas,double pesoArchivosMb) {

        this.nodeId = nodeId;
        this.filename = filename;
        this.contentType = contentType;
        this.extension = extension;
        this.numeroPaginas = numeroPaginas;
        this.pesoArchivosMb = pesoArchivosMb;
    }

    public UploadResponse(Builder builder){
        this.nodeId = builder.nodeId;
        this.filename = builder.filename;
        this.contentType = builder.contentType;
        this.extension = builder.extension;
        this.numeroPaginas = builder.numeroPaginas;
        this.pesoArchivosMb = builder.pesoArchivosMb;
    }

    public static class Builder {

        private String nodeId;
        private String filename;
        private String contentType;
        private String extension;
        private Integer numeroPaginas;
        private double pesoArchivosMb;

        @SneakyThrows
        public Builder(MultipartFile archivo, String nodeId) {

            this.nodeId = nodeId;
            this.extension = FilenameUtils.getExtension(archivo.getOriginalFilename());
            this.filename = archivo.getOriginalFilename();
            this.contentType = archivo.getContentType();
            this.pesoArchivosMb = archivo.getSize() / 1048576.0;
            this.numeroPaginas = extension.equals(Constante.TIPO_PDF) ? UtilArchivo.getPagesNumber(archivo.getBytes()) : 1;
        }

        public UploadResponse build() {
            return new UploadResponse(this);
        }
    }



}