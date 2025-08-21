package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadFileRequest {
    private String filename;
}
