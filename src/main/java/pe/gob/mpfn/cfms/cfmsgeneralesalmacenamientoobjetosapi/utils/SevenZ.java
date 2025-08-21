package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.utils;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SevenZ {

    private SevenZ() {

    }

    public static void compress(String name, File... files) throws IOException {
        try (SevenZOutputFile out = new SevenZOutputFile(new File(name))){
            for (File file : files){
                addToArchiveCompression(out, file, ".");
            }
        }
    }

    public static void compressMultipart(String name, List<MultipartFile> files) throws IOException {

        try (SevenZOutputFile out = new SevenZOutputFile(new File(name))){

            for (MultipartFile file : files){
                addToArchiveCompression(out,convertMultiPartToFile(file) , ".");

            }
        }
    }


    public static void decompress(String in, File destination) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(new File(in));
        SevenZArchiveEntry entry;
        while ((entry = sevenZFile.getNextEntry()) != null){
            if (entry.isDirectory()){
                continue;
            }
            File curfile = new File(destination, entry.getName());
            File parent = curfile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(curfile);
            byte[] content = new byte[(int) entry.getSize()];
            sevenZFile.read(content, 0, content.length);
            out.write(content);
            out.close();
        }
    }

    private  static void addToArchiveCompression(SevenZOutputFile out, File file, String dir) throws IOException {

        String name = dir + File.separator + file.getName();

        if (file.isFile()){
            SevenZArchiveEntry entry = out.createArchiveEntry(file, name);
            out.putArchiveEntry(entry);

            FileInputStream in = new FileInputStream(file);
            byte[] b = new byte[1024];
            int count = 0;
            while ((count = in.read(b)) > 0) {
                out.write(b, 0, count);
            }
            out.closeArchiveEntry();



        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null){
                for (File child : children){
                    addToArchiveCompression(out, child, name);
                }
            }
        } else {
            System.out.println(file.getName() + " is not supported");
        }

    }


    public static File convertMultiPartToFile(MultipartFile file ) throws IOException {
        File convFile = new File( file.getOriginalFilename() );
        FileOutputStream fos = new FileOutputStream( convFile );
        fos.write( file.getBytes() );
        fos.close();
        return convFile;
    }


    private static void eliminarArchivo(String ruta)  throws IOException {
        String file_name = ruta;
        Path path = Paths.get(file_name);
        Files.delete(path);

    }


    public static byte[] comprimir(List<MultipartFile> archivos) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel();
             SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(inMemoryByteChannel)) {

            for (MultipartFile archivo : archivos) {

                byte[] archivoBytes = archivo.getBytes();
                String nombreArchivo = archivo.getOriginalFilename();
                SevenZArchiveEntry entry = new SevenZArchiveEntry();
                entry.setName(nombreArchivo);
                entry.setSize(archivoBytes.length);
                sevenZOutputFile.putArchiveEntry(entry);
                sevenZOutputFile.write(archivoBytes);
                sevenZOutputFile.closeArchiveEntry();
            }

            sevenZOutputFile.finish();
            inMemoryByteChannel.position(0);

            ByteBuffer buffer = ByteBuffer.allocate((int) inMemoryByteChannel.size());
            inMemoryByteChannel.read(buffer);

            return buffer.array();
        } catch (IOException e) {
            // Manejar la excepción según tus necesidades
            e.printStackTrace();
            return null;
        }

    }

    public static byte[] descomprimir(byte[] compressedData) throws IOException {
        try (SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(compressedData);
             SevenZFile sevenZFile = new SevenZFile(channel);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                byte[] content = new byte[(int) entry.getSize()];
                sevenZFile.read(content, 0, content.length);
                outputStream.write(content);
            }
            
            return outputStream.toByteArray();
        }
    }

}