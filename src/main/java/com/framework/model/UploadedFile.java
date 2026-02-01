package com.framework.model;

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

    @Override
    public String toString() {
        return String.format("UploadedFile{name='%s', type='%s', size=%d}", fileName, contentType, size);
    }
}
