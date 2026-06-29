package org.automation.api.rest.commons.utils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.io.InputStream;
import java.util.Objects;

public class JsonUtil {

    public static DocumentContext getDocumentContext(String json) {return JsonPath.parse(json);}

    public static DocumentContext getDocumentContext(InputStream json) {return JsonPath.parse(json);}

    public static String getNodeValue(String json, String path){
        return Objects.isNull(getDocumentContext(json).read("$."+path))?null
                : getDocumentContext(json).read("$."+path).toString();
    }

    public static String getNodeValue(InputStream json, String path){
        return Objects.isNull(getDocumentContext(json).read("$."+path))?null
                : getDocumentContext(json).read("$."+path).toString();
    }

    public static String setNodeValue(String json, String path, String valueToReplace){
        return getDocumentContext(json).set("$."+path, valueToReplace).jsonString();
    }

}
