package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto;

import org.springframework.http.HttpStatus;
import lombok.Data;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils.Message;

@Data
public class ResponseDto<T> {
    private String mensaje;
    private T data;
    private int codigo = HttpStatus.OK.value();

    public ResponseDto(T data, Message mensaje) {
        this.mensaje = mensaje.getMessage();
        this.data = data;
        this.codigo = mensaje.getCode();
    }

    public ResponseDto(Message mensaje) {
        this.mensaje = mensaje.getMessage();
        this.codigo = mensaje.getCode();
    }

    public ResponseDto() {
    }
}
