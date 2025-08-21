package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class UtilArchivo {

    private static final Map<String, String> mimeTypes = new HashMap<>();

    static {
        mimeTypes.put("mov", "video/quicktime");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("mp4", "video/mp4");
        mimeTypes.put("avi", "video/x-msvideo");
        mimeTypes.put("webm", "video/webm");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("aac", "audio/aac");
        mimeTypes.put("ogg", "audio/ogg");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("mp3", "audio/mpeg");

    }


    public static int getPagesNumber(byte[] file) {
        int pageCount = 0;
        try {
            PDDocument pdfDoc = Loader.loadPDF(file);
            pageCount = pdfDoc.getNumberOfPages();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return pageCount;
    }


    public static String calcularHash(byte[] archivoBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");  // Algoritmo de hash SHA-256
        byte[] hashBytes = digest.digest(archivoBytes);  // Calcular el hash
        return Hex.encodeHexString(hashBytes);  // Convertir los bytes del hash a hexadecimal
    }



    public static String getContentTypeByExtension(String extension) {
        return mimeTypes.getOrDefault(extension.toLowerCase(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

}
