package com.ali.almashplayer;

public class LiveChannel {

    private String name;
    private String info;
    private String url;

    public LiveChannel(String name, String info, String url) {
        this.name = name;
        this.info = info;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getInfo() {
        return info;
    }

    public String getUrl() {
        return url;
    }
}
