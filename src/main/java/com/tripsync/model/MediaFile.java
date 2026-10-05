package com.tripsync.model;

import java.time.LocalDateTime;

public class MediaFile {
    private Long id;
    private Long memoryId;
    private String fileUrl;
    private FileType fileType;
    private String fileName;
    private LocalDateTime createdAt;

    public enum FileType { IMAGE, VIDEO }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMemoryId() { return memoryId; }
    public void setMemoryId(Long memoryId) { this.memoryId = memoryId; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
