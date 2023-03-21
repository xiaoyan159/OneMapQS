package com.navinfo.collect.library.theme;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.List;
import java.util.Map;

public class NavThemeUtils {
    public String loadThemeEntity(String documentStr, List<ThemeEntity> themeEntityList) throws DocumentException {
        if (documentStr == null) {
            return null;
        }
        if (themeEntityList == null) {
            return documentStr;
        }
        Document document = DocumentHelper.parseText(documentStr);
        Element rootElement = document.getRootElement();
        for (int i = 0; i < themeEntityList.size(); i++) {
            ThemeEntity themeEntity = themeEntityList.get(i);
            parserElement(themeEntity, rootElement);
        }
        return rootElement.asXML();
    }

    private Element parserElement(ThemeEntity themeEntity, Element element) {
        if (themeEntity.getName()!=null&&!themeEntity.getName().trim().isEmpty()) {
            Element currentElement = element.addElement(themeEntity.getName());
            if (themeEntity.getProperties()!=null) {
                for (Map.Entry<String, String> entry: themeEntity.getProperties().entrySet()) {
                    currentElement = currentElement.addAttribute(entry.getKey(), entry.getValue());
                }
            }
            if (themeEntity.getChildren()!=null) {
                for (ThemeEntity childEntity: themeEntity.getChildren()) {
                    parserElement(childEntity, currentElement);
                }
            }
            return currentElement;
        }
        return element;
    }
}
