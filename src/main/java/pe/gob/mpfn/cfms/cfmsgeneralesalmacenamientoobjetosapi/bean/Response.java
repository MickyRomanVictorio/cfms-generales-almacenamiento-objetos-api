package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    String nombreArchivo;
    int numeroFolios = 0;

    public Response(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }
}
