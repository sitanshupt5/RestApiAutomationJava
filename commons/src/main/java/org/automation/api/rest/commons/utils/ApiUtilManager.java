package org.automation.api.rest.commons.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.automation.api.rest.commons.constants.ConfigConstants;
import org.automation.api.rest.commons.constants.Entity;
import org.automation.api.rest.commons.constants.FilePaths;
import org.automation.api.rest.commons.enums.ApiContext;
import org.automation.api.rest.commons.httpservicemanager.ConfigManager;
import org.automation.api.rest.commons.stepdefs.TestManagerContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.*;

public class ApiUtilManager {

    private static final Logger logger = LoggerFactory.getLogger(ApiUtilManager.class);

    public String request;

    public String env = ConfigManager.getSystemPropertyOrSetDefault(ConfigConstants.ENV_TYPE, ConfigConstants.DEFAULT_ENV).split("-")[0];

    public static String getJsonNodeValue(String jsonString, String nodeKey) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode data = mapper.readTree(jsonString);
        JsonNode node = data.isArray() ? data.get(0) : data;
        if (node == null) {
            return null;
        }
        if (nodeKey.contains("/")) {
            return node.at(nodeKey) == null ? null : node.at(nodeKey).asText();
        } else {
            return node.get(nodeKey) == null ? null : node.get(nodeKey).asText();
        }
    }

    public String getRequestBody(TestManagerContext testManagerContext, String customer) throws IOException, URISyntaxException, ParseException {
        ObjectMapper mapper = new ObjectMapper();
        String api = (String) testManagerContext.getScenarioContext().getContext(ApiContext.API_NAME);
        ObjectNode defaults = getDefaults(api);
        if (defaults.get("request") != null) {
            String requestTemplate = defaults.get("request").toPrettyString();
            JSONObject payload = new JSONObject(requestTemplate);
            if (!Strings.isNullOrEmpty(customer)) {
                ObjectReader updater = mapper.readerForUpdating(defaults.get(Entity.REQUEST));
                JSONObject data = new JSONObject(updater.readTree(mapper.writeValueAsString(getData(testManagerContext, customer, api))).toPrettyString());
                logger.info("Data:\n" + data.toString());
                request = mapData(payload, data);
                logger.info("Final Request Body:\n" + request);

            } else {
                request = defaults.get(Entity.REQUEST).toString();
            }
            return request.replace("{RandomString}", getRandomAlphaString()).replace("{ID}", (String) testManagerContext.getScenarioContext().getContext(ApiContext.ID)).replace("{RandomUUID}", getUniqueCorrelationid()).replace("{currentdate}", DateUtils.getTodayDateInString());
        }
        return null;
    }

    public String mapData(Object payload, JSONObject data) {
        String payloadTxt = payload.toString();
        for (String key : data.keySet()) {
            if (payloadTxt.contains("{" + key + "}")) {
                Object obj = data.get(key);
                if (obj instanceof Integer) {
                    String value = "{" + key + "}";
                    payloadTxt = payloadTxt.replace("\"" + value + "\"", obj.toString());

                } else if (obj instanceof Boolean) {
                    String value = "{" + key + "}";
                    payloadTxt = payloadTxt.replace("\"" + value + "\"", obj.toString());

                } else {
                    if (obj.toString().equals("null")) {
                        String value = "{" + key + "}";
                        payloadTxt = payloadTxt.replace("\"" + value + "\"", "null");

                    } else {
                        payloadTxt = payloadTxt.replace("{" + key + "}", obj.toString());
                    }
                }
            }
        }
        payloadTxt = payloadTxt.replaceAll("\"\\[(.*?)]\"", "EmptyObject");
        return removeEmptyJsonElements(new JSONTokener(payloadTxt).nextValue());
    }

    private String removeEmptyJsonElements(Object object) {
        if (object instanceof JSONArray) {
            JSONArray array = (JSONArray) object;
            int num = array.length() - 1;
            for (; num >= 0; num--) {
                if (array.get(num).toString().equals("{}")) {
                    array.remove(num);
                } else {
                    try {
                        removeEmptyJsonElements(array.getJSONObject(num));
                    } catch (JSONException e) {
                        if (array.get(num) instanceof String)
                            if (array.get(num).equals("EmptyObject")) array.remove(num);
                    }
                }
            }

        } else if (object instanceof JSONObject) {
            final JSONObject jsonObject = (JSONObject) object;
            JSONArray names = jsonObject.names();
            Iterator<Object> els = names.iterator();
            while (els.hasNext()) {
                String key = els.next().toString();
                if (jsonObject.get(key).toString().equals("EmptyObject") || jsonObject.isEmpty() || jsonObject.get(key).toString().equals("[]")) {
                    jsonObject.remove(key);

                } else {
                    removeEmptyJsonElements(jsonObject.get(key));
                }
            }
            JSONArray nmes = jsonObject.names();
            if (nmes != null) {
                Iterator<Object> keys = nmes.iterator();
                while (keys.hasNext()) {
                    String key = keys.next().toString();
                    if (jsonObject.get(key) instanceof JSONObject) {
                        if (jsonObject.get(key).toString().equals("{}")) {
                            jsonObject.remove(key);

                        } else if (jsonObject.get(key).toString().equals("[]")) {
                            jsonObject.remove(key);

                        } else {
                            removeEmptyJsonElements(jsonObject.get(key));
                        }
                    }
                }
            }
        }
        return object.toString();
    }

    public Map<String, String> getHeader(String api, String customerID) throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode defaults = getDefaults(api);
        return mapper.convertValue(defaults.get(Entity.HEADER), new TypeReference<Map<String, String>>() {
        });
    }

    public String getBasePath(String api) throws IOException, URISyntaxException {
        ObjectNode defaults = getDefaults(api);
        return getJsonNodeValue(defaults, Entity.BASE_PATH);
    }

    public String mapQueryData(String query, Map<String, Object> mp) {
        for (String key : mp.keySet()) {
            if (query.contains("{" + key + "}")) {
                query = query.replace("{" + key + "}", mp.get(key).toString());
            }
        }
        return query;
    }

    public void setEntityHostURI(String api, TestManagerContext testManagerContext) throws IOException, URISyntaxException {
        ObjectNode defaults = getDefaults(api);
        String entity_host_uri = testManagerContext.configManager.getEnvProperty(ConfigConstants.ENTITY_HOST_URI);
        if (!Strings.isNullOrEmpty(entity_host_uri)) {
            testManagerContext.configManager.put(ConfigConstants.ENTITY_HOST_URI, "");
        }
        entity_host_uri = getJsonNodeValue(defaults, Entity.HOST_URI);
        if (!Strings.isNullOrEmpty(entity_host_uri)) {
            testManagerContext.configManager.put(ConfigConstants.ENTITY_HOST_URI, entity_host_uri);
        }
    }

    public Map<String, String> getParams(String api) throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode defaults = getDefaults(api);
        return mapper.convertValue(defaults.get(Entity.PARAMS), new TypeReference<Map<String, String>>() {
        });
    }

    public Map<String, String> getAuthParams(String api) throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode defaults = getDefaults(api);
        return mapper.convertValue(defaults.get(Entity.AUTH_PARAMS), new TypeReference<Map<String, String>>() {
        });
    }

    public Map<String, String> getQueryParams(TestManagerContext testManagerContext, String customer) throws IOException, URISyntaxException, ParseException {
        ObjectMapper mapper = new ObjectMapper();
        String api = (String) testManagerContext.getScenarioContext().getContext(ApiContext.API_NAME);
        ObjectNode defaults = getDefaults(api);
        if (customer == null) {
            return mapper.convertValue(defaults.get(Entity.QUERY_PARAMS), new TypeReference<Map<String, String>>() {
            });
        }
        if (defaults.get("query_params") != null) {
            String queryParams = defaults.get("query_params").toPrettyString();
            JSONObject payload = new JSONObject(queryParams);
            if (!Strings.isNullOrEmpty(customer)) {
                ObjectReader updater = mapper.readerForUpdating(defaults.get(Entity.QUERY_PARAMS));
                JSONObject data = new JSONObject(updater.readTree(mapper.writeValueAsString(getData(testManagerContext, customer, api))).toPrettyString());
                logger.info("Data:\n" + data.toString());
                request = mapData(payload, data);
                logger.info("Final Request Body:\n" + request);
                if (request.equals("{}")) {
                    return null;
                }
                HashMap<String, String> map = new HashMap<>();
                JSONObject jsonObject = new JSONObject(request);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = jsonObject.get(key);
                    map.put(key, String.valueOf(value));
                }
                return map;
            }
        }
        return null;
    }

    public Map<String, Object> getSchema(String schemaKey, String api) {
        String filePath = FilePaths.SCHEMA_MAPPING.replace(Entity.API_PATH, api).replace(Entity.ENV_TYPE, env);
        YamlReaderUtils yamlReaderUtils = new YamlReaderUtils(ClassLoader.getSystemResourceAsStream(filePath));
        return yamlReaderUtils.getYamlObj(schemaKey);
    }

    public String getValue(String schemaKey, String api) {
        String filePath = FilePaths.SCHEMA_MAPPING.replace(Entity.API_PATH, api).replace(Entity.ENV_TYPE, env);
        YamlReaderUtils yamlReaderUtils = new YamlReaderUtils(ClassLoader.getSystemResourceAsStream(filePath));
        return (String) yamlReaderUtils.getValue(schemaKey);
    }

    public List<String> getListValue(String schemaKey, String api) {
        String filePath = FilePaths.SCHEMA_MAPPING.replace(Entity.API_PATH, api).replace(Entity.ENV_TYPE, env);
        YamlReaderUtils yamlReaderUtils = new YamlReaderUtils(ClassLoader.getSystemResourceAsStream(filePath));
        return (List<String>) yamlReaderUtils.getList(schemaKey);
    }

    public String getJsonNodeValue(JsonNode data, String nodeKey) {
        JsonNode node = data.isArray() ? data.get(0) : data;
        if (node == null) {
            return null;
        }
        if (nodeKey.contains("/")) {
            return node.at(nodeKey) == null ? null : node.at(nodeKey).asText();
        } else {
            return node.get(nodeKey) == null ? null : node.get(nodeKey).asText();
        }
    }

    public String getUniqueCorrelationid() {
        return UUID.randomUUID().toString();
    }

    public String getRandomAlphaString() {
        int length = 10;
        String candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }
        return sb.toString();
    }

    public ObjectNode getDefaults(String api) throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(getClass().getResource(FilePaths.API_PATH_REQUEST_JSON.replace(Entity.API_PATH, api).replace(Entity.ENV_TYPE, env)).toURI()), ObjectNode.class);
    }

    public String getTestDataFilePath(String api) {
        return FilePaths.TEST_DATA_FILE_PATH.replace(Entity.API_PATH, api).replace(Entity.ENV_TYPE, env);
    }

    public void setDataSet(TestManagerContext testManagerContext, String customer, String api) {
        String filePath = getTestDataFilePath(api);
        filePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(filePath);
        if (inputStream == null) {
            throw new RuntimeException("YAML file not found at path: " + filePath);
        }
        YamlReaderUtils yamlReaderUtils = new YamlReaderUtils(inputStream);
        testManagerContext.getScenarioContext().setContext(ApiContext.DATASET, yamlReaderUtils.getValue(customer));
    }

    public Object getData(TestManagerContext testManagerContext, String customer, String api) {
        setDataSet(testManagerContext, customer, api);
        return testManagerContext.getScenarioContext().getContext(ApiContext.DATASET);
    }

    public JSONObject readDataFile(TestManagerContext testManagerContext, String dataset, String api) throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader updater = mapper.readerForUpdating(getDefaults(api).get(Entity.REQUEST));
        return new JSONObject(updater.readTree(mapper.writeValueAsString(getData(testManagerContext, dataset, api))).toPrettyString());
    }
}