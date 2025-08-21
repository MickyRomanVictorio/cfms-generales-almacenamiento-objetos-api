package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto;


import lombok.Data;

@Data
public class ArchivoResponse {

    private String archivoId;
    private String hash;
    private double pesoArchivosMb;


    public ArchivoResponse(
            String archivoId,
            String hash ,
            double pesoArchivosMb) {
        this.archivoId = archivoId;
        this.hash = hash;
        this.pesoArchivosMb = pesoArchivosMb;
    }


}