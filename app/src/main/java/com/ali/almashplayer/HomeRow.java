package com.ali.almashplayer;

import java.util.List;

public class HomeRow {
    private String title;
    private List<HomeItem> items;

    public HomeRow(String title, List<HomeItem> items) {
        this.title = title;
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public List<HomeItem> getItems() {
        return items;
    }
}
