package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class FileSystemMultipartFile implements MultipartFile {

    private final File file;
    private final String originalFilename;
    private final String contentType;

    public FileSystemMultipartFile(File file, String contentType) {
        this.file = file;
        this.originalFilename = file.getName();
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return originalFilename;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return file.length() == 0;
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return is.readAllBytes();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (InputStream is = new FileInputStream(file);
             OutputStream os = new FileOutputStream(dest)) {
            is.transferTo(os);
        }
    }
}