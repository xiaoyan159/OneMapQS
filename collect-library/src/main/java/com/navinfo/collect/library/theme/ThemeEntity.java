package com.navinfo.collect.library.theme;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeEntity {
    @NotNull
    private String name = "m";
    private Map<String, String> properties = new HashMap<>();
    private List<ThemeEntity> children = new ArrayList<>();

    public ThemeEntity() {
    }

    public ThemeEntity(String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<ThemeEntity> getChildren() {
        return children;
    }

    public void setChildren(List<ThemeEntity> children) {
        this.children = children;
    }

}
