package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.commons;


import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.dto.InfoResponse;
@ControllerAdvice
public class FileUploadExceptionAdvice {


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<InfoResponse> handleMaxSizeException(MaxUploadSizeExceededException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InfoResponse("verifica el tamaño de los archivos"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InfoResponse> handleException(Exception ex){

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InfoResponse(ex.getMessage()));
    }
}
