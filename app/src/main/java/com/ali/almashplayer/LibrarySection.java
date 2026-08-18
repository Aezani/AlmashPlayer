package com.ali.almashplayer;

import java.util.ArrayList;
import java.util.List;

public class LibrarySection {

    private String key;        // مثل "7"
    private String title;      // مثل "أفلام انيمي"
    private String type;       // movie / show
    private List<String> paths = new ArrayList<>();

    public LibrarySection(String key, String title, String type) {
        this.key = key;
        this.title = title;
        this.type = type;
    }

    public void addPath(String path) {
        paths.add(path);
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public List<String> getPaths() {
        return paths;
    }
}
