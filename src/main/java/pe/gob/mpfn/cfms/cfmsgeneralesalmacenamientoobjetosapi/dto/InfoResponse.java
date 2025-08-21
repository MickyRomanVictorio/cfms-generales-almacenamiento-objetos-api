package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter @Setter
public class InfoResponse {

    private int code;
    private String message;
    private String nombrezip;

    public InfoResponse() {
    }

    public InfoResponse(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public InfoResponse(String message) {
        this.message = message;
    }

    public int getCode() {
        if ( code == 0 ) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return code;
    }

    public String getNombrezip() {
        return nombrezip;
    }

    public void setNombrezip(String nombrezip) {
        this.nombrezip = nombrezip;
    }
}