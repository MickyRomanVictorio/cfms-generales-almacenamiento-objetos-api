package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.JavaLayerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.poi.hwpf.HWPFDocument;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Slf4j
@Component
public class ArchivoUtil {

  public boolean esArchivoValido(MultipartFile file) {

    String contentType = file.getContentType();

    boolean isValid = false;

    switch(Objects.requireNonNull(contentType)) {
      case Constante.CONTENT_TYPE_IMAGE_JPEG, Constante.CONTENT_TYPE_IMAGE_PNG:
        isValid = isValidImage(file);
        break;
      case Constante.CONTENT_TYPE_APPLICATION_PDF:
        isValid = isValidPdf(file);
        break;
      case Constante.CONTENT_TYPE_APPLICATION_MSWORD:
        isValid = isValidDoc(file);
        break;
      case Constante.CONTENT_TYPE_VIDEO_MP4, Constante.CONTENT_TYPE_VIDEO_X_MATROSKA,
           Constante.CONTENT_TYPE_VIDEO_QUICKTIME, Constante.CONTENT_TYPE_VIDEO_X_MSVIDEO:
        isValid = isValidVideo(file);
        break;
      case Constante.CONTENT_TYPE_AUDIO_WAV, Constante.CONTENT_TYPE_AUDIO_OGG:
        isValid = isValidAudio(file);
        break;
      default:
        break;
    }

    return isValid;

  }

  public boolean esTipoArchivoValido(MultipartFile file) {

    String contentType = file.getContentType();

    boolean isValid;

    switch(Objects.requireNonNull(contentType)) {
      case Constante.CONTENT_TYPE_IMAGE_JPEG, Constante.CONTENT_TYPE_IMAGE_PNG, Constante.CONTENT_TYPE_APPLICATION_PDF,
           Constante.CONTENT_TYPE_APPLICATION_MSWORD, Constante.CONTENT_TYPE_VIDEO_MP4,
           Constante.CONTENT_TYPE_VIDEO_X_MATROSKA, Constante.CONTENT_TYPE_VIDEO_QUICKTIME,
           Constante.CONTENT_TYPE_VIDEO_X_MSVIDEO, Constante.CONTENT_TYPE_AUDIO_WAV, Constante.CONTENT_TYPE_AUDIO_OGG:
        isValid = true;
        break;
      default:
        isValid = false;
        break;
    }

    return isValid;

  }

  public String calcularHash(MultipartFile file) {

    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(Constante.SHA_256);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    byte[] hashBytes;
    try {
      hashBytes = digest.digest(file.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return Hex.encodeHexString(hashBytes);
  }

  private boolean isValidImage(MultipartFile file) {
    try {
      BufferedImage image = ImageIO.read(file.getInputStream());
      return image != null;
    } catch (IOException e) {
      return false;
    }
  }


  private boolean isValidPdf(MultipartFile file) {

    try {
      // Cargar el PDF usando PdfReader con los bytes del archivo
      PdfReader pdfReader = new PdfReader(file.getBytes());

      // Extraer texto de la primera página para verificar si se puede leer
      String textFromPage = PdfTextExtractor.getTextFromPage(pdfReader, 1);

      // Considerar el PDF válido si se pudo extraer algún texto
      return textFromPage != null && !textFromPage.isEmpty();
    } catch (IOException e) {
      // Si hay una excepción (por ejemplo, archivo corrupto o no es un PDF), es inválido
      return false;
    }
  }

  private boolean isValidDoc(MultipartFile file) {
    try {
      HWPFDocument document = new HWPFDocument(file.getInputStream());
      return document.getSummaryInformation() != null;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean isValidVideo(MultipartFile file) {
    File tempFile = null;
    FileChannelWrapper ch = null;
    try {
      // Crear archivo temporal
      tempFile = File.createTempFile("temp_video", ".mp4");

      // Escribir el contenido del MultipartFile en el archivo temporal
      try (InputStream inputStream = file.getInputStream();
           FileOutputStream out = new FileOutputStream(tempFile)) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          out.write(buffer, 0, bytesRead);
        }
      }

      // Usar JCodec para intentar leer el archivo de video
      ch = NIOUtils.readableFileChannel(String.valueOf(tempFile));
      FrameGrab grab = FrameGrab.createFrameGrab(ch);

      // Intentar obtener el primer cuadro para verificar si es un archivo válido
      Picture picture = grab.getNativeFrame();

      // Si obtenemos un cuadro, el video es válido
      return picture != null;
    } catch (IOException e) {
      return false;
    } catch (JCodecException e) {
        throw new RuntimeException(e);
    } finally {
      // Limpiar recursos
      if (ch != null) {
        IOUtils.closeQuietly(ch);
      }
      if (tempFile != null && tempFile.exists()) {
        tempFile.delete();
      }
    }
  }

  private boolean isValidAudio(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      Bitstream bitstream = new Bitstream(inputStream);
      return bitstream.readFrame() != null;
    } catch (IOException | JavaLayerException e) {
      return false;
    }
  }
}
