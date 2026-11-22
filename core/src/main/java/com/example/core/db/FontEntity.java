package com.example.core.db;

import androidx.room3.Entity;
import androidx.room3.PrimaryKey;

@Entity
public class FontEntity {
    @PrimaryKey(autoGenerate = true)
    private long Id;
    private String name;
    private Long dateAdded;
    private Long size;
    private String path;

    public FontEntity(Long dateAdded, String name, Long size, String path) {
        this.dateAdded = dateAdded;
        this.name = name;
        this.size = size;
        this.path = path;
    }

    public Long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getId() {
        return Id;
    }

    public void setId(long id) {
        Id = id;
    }
}
