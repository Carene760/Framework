
package core.model;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UploadedFile {
    private String fileName;
    private String contentType;
    private long size;
    private byte[] content;
    
    public UploadedFile() {}
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
    
    public boolean isEmpty() {
        return fileName == null || fileName.isEmpty() || size == 0;
    }
    
    public String getExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    public void readFromInputStream(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            
            this.content = buffer.toByteArray();
            this.size = this.content.length;
            buffer.close();
        }
    }
    
    @Override
    public String toString() {
        return String.format("UploadedFile{name='%s', type='%s', size=%d}", 
            fileName, contentType, size);
    }
}