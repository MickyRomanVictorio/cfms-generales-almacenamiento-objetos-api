package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils;

public enum Message {



    ARCHIVO_ELIMINADO("Archivo eliminado exitosamente.",200),
    ERROR_ELIMINAR_ARCHIVO( "Error al eliminar el archivo." ,500),
    ERROR_SUBIDA_ARCHIVO("Error en la subida de archivo",500),
    ARCHIVO_SUBIDO("El archivo fue cargado correctamente", 200),
    ARCHIVO_CORRUPTO("Archivo PDF no válido o corrupto.", 400),

    ARCHIVO_VALIDO("Archivo valido y permitido.",200),
    C_42201065_TIPOARCHIVO_INVALIDO("Tipo de archivo no permitido.", 42201065),
    C_42201064_ARCHIVO_INVALIDO("Archivo no válido o corrupto.", 42201064),


    TOKEN_INVALIDO("Token inválido o expirado", 401);

    String message;
    int code;

    String mensajeAdicional;

    private Message(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public void setMensajeAdicional(String mensajeAdicional) {
        this.mensajeAdicional = mensajeAdicional;
        this.message = this.message + this.mensajeAdicional;
    }

}