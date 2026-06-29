package org.automation.api.rest.commons.httpservicemanager;

import com.google.common.base.Strings;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

public class HttpServiceAssertion {

    private HttpResponseManager httpResponseManager;

    private static final Logger logger =
            LoggerFactory.getLogger(HttpServiceAssertion.class);

    public HttpServiceAssertion(HttpResponseManager http) {
        httpResponseManager = http;
    }

    public void statusCodeIs(final int expectedStatusCode) {
        int actualStatusCode =
                httpResponseManager.getResponse().getStatusCode();

        String assertionReason =
                "Expected Status code should match the actual status code.";

        assertThat(assertionReason,
                actualStatusCode,
                is(equalTo(expectedStatusCode)));

        logger.info("Expected HTTP status code: {} matches the actual response status code: {}",
                expectedStatusCode,
                actualStatusCode);
    }

    public void statusCodeIsNot(int statusCode) {
        int actualStatusCode =
                httpResponseManager.getResponse().getStatusCode();

        String assertionReason =
                "Expected status code should not match the actual status code.";

        assertThat(assertionReason,
                actualStatusCode,
                is(not(equalTo(statusCode))));
    }

    public void bodyContainsPropertyWithValue(String propertyPath,
                                              String expectedPropertyValue) {

        JsonPath actualResponse =
                httpResponseManager.getResponse().jsonPath();

        String propertyValueFromResponse = null;

        if (!Strings.isNullOrEmpty(
                actualResponse.getString(propertyPath))) {

            propertyValueFromResponse =
                    actualResponse.getString(propertyPath)
                            .replaceAll("\\[", "")
                            .replaceAll("\\]", "");
        }

        String assertionReason = String.format(
                "Response field '%s' value is not equal to '%s' value.",
                propertyPath,
                expectedPropertyValue);

        assertThat(assertionReason,
                propertyValueFromResponse,
                is(equalTo(expectedPropertyValue)));

        logger.info("Response field value for {} is equal to {}",
                propertyPath,
                expectedPropertyValue);
    }

    public void bodyContainsPropertyWithExpectedValue(String propertyPath, Object expectedPropertyValue) {

        String actualResponse = httpResponseManager.getResponse().asString();
        String propertyValueFromResponse = null;
        if ((com.jayway.jsonpath.JsonPath.read(actualResponse,"$." + propertyPath) != null)) {
            if (!com.jayway.jsonpath.JsonPath.read(actualResponse,"$." + propertyPath).toString().equals("[null]")) {
                Object obj = com.jayway.jsonpath.JsonPath.read(actualResponse,"$." + propertyPath);

                if (obj instanceof Double) {
                    Double d = Double.parseDouble(obj.toString());
                    propertyValueFromResponse =
                            BigDecimal.valueOf(d).toString();
                } else if (obj instanceof Boolean) {
                    Boolean bool = Boolean.parseBoolean(obj.toString());
                    propertyValueFromResponse = bool.toString();
                } else if (obj instanceof Integer) {
                    int d = Integer.parseInt(obj.toString());
                    propertyValueFromResponse = String.valueOf(d);
                } else {
                    propertyValueFromResponse = com.jayway.jsonpath.JsonPath.read(
                            actualResponse,"$." + propertyPath)
                            .toString();
                }

                if (propertyValueFromResponse.equals("\\[.*") || propertyValueFromResponse.contains("\\\"")) {
                    propertyValueFromResponse = propertyValueFromResponse.replace("[", "")
                                    .replace("]", "")
                                    .replace("\\", "");
                }
            }
        }

        if (expectedPropertyValue == JSONObject.NULL) {

            String assertionReason = String.format(
                    "Response field '%s' value is not equal to '%s' value.",
                    propertyPath,
                    expectedPropertyValue);

            assertThat(assertionReason, propertyValueFromResponse, is(equalTo(null)));

            logger.info(
                    "Response field value for {} is : {}."
                            + "\nExpected property value for the property path is: {}."
                            + "\nHence, expected property value matches the actual property value.",
                    propertyPath,
                    propertyValueFromResponse,
                    expectedPropertyValue);

        } else if (expectedPropertyValue.equals("null")) {

            String assertionReason = String.format(
                    "Response field '%s' value is not equal to '%s' value.",
                    propertyPath,
                    expectedPropertyValue);

            assertThat(assertionReason, propertyValueFromResponse, is(equalTo(expectedPropertyValue)));

            logger.info(
                    "Response field value for {} is : {}."
                            + "\nExpected property value for the property path is: {}."
                            + "\nHence, expected property value matches the actual property value.",
                    propertyPath,
                    propertyValueFromResponse,
                    expectedPropertyValue);

        } else {

            if (propertyValueFromResponse != null) {
                propertyValueFromResponse = propertyValueFromResponse.replaceAll("[\\r\\n]+", "");
            }
            String assertionReason = String.format(
                    "Response field '%s' value is not equal to '%s' value.",
                    propertyPath,
                    expectedPropertyValue);
            assertThat(assertionReason,
                    propertyValueFromResponse,
                    is(equalTo(expectedPropertyValue.toString())));

            logger.info(
                    "Response field value for {} is : {}."
                            + "\nExpected property value for the property path is: {}."
                            + "\nHence, expected property value matches the actual property value.",
                    propertyPath,
                    propertyValueFromResponse,
                    expectedPropertyValue);
        }
    }

    public void bodyDoesNotContainPath(String path) {
        Object responsePath =
                httpResponseManager.getResponse()
                        .jsonPath()
                        .get(path);

        assertThat(responsePath, nullValue());
    }

    public void bodyContainsKey(String key)
            throws InterruptedException {

        String propertyVal =
                httpResponseManager.getResponse()
                        .jsonPath()
                        .getString(key);

        assertThat("Key not found",
                !(Strings.isNullOrEmpty(propertyVal)));
    }

    public void validateTheJsonResponseSchema(String schemaPath) {
        httpResponseManager.getResponse()
                .then()
                .assertThat()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaPath));
    }

    public String getJsonPathValue(String jsonPath) {
        JsonPath actualResponse =
                httpResponseManager.getResponse().jsonPath();

        return actualResponse.getString(jsonPath);
    }

    public List<Object> getListOfJsonPathValue(String jsonPath) {
        JsonPath actualResponse =
                httpResponseManager.getResponse().jsonPath();

        return actualResponse.getList(jsonPath);
    }

    public List<Map<String, Object>> getListOfJsonPathValue1(
            String jsonPath) {

        JsonPath actualResponse =
                httpResponseManager.getResponse().jsonPath();

        return actualResponse.getList(jsonPath);
    }

    public List<Map<String, String>> getResponseAsList() {
        return httpResponseManager.getResponse().as(List.class);
    }

    public List<Map<String, Object>> getResponseAsList1() {
        return httpResponseManager.getResponse().as(List.class);
    }

    public void propertyContainsValue(String propertyPath,
                                      Object containedValue) {

        String actualResponse = httpResponseManager.getResponse().asString();

        String propertyValueFromResponse = null;

        if(!(com.jayway.jsonpath.JsonPath.read(actualResponse, "$." + propertyPath)==null)) {
            if(!com.jayway.jsonpath.JsonPath.read(actualResponse, "$."+propertyPath).toString().contains("[null]"))
            {
                Object obj = com.jayway.jsonpath.JsonPath.read(actualResponse, "$."+propertyPath);
                if(obj instanceof Double) {
                    Double d = Double.parseDouble(obj.toString());
                    propertyValueFromResponse = BigDecimal.valueOf(d).toString();
                }
                else if(obj instanceof Boolean) {
                    Boolean bool = Boolean.parseBoolean(obj.toString());
                    propertyValueFromResponse = bool.toString();
                }
                else if(obj instanceof Integer) {
                    int d = Integer.parseInt(obj.toString());
                    propertyValueFromResponse = Integer.toString(d);
                }
                else {
                    propertyValueFromResponse = com.jayway.jsonpath.JsonPath.read(actualResponse, "$."+propertyPath).toString();
                }
                if(propertyValueFromResponse.equals("[.*")||propertyValueFromResponse.contains("\""))
                {
                    propertyValueFromResponse = propertyValueFromResponse.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "");
                }
            }
        }

        if (containedValue.toString().contains(",")) {

            String[] valuesArray = containedValue.toString().split(",");

            for (String value : valuesArray) {
                String assertionReason = String.format("Response field '%s' does not contain value '%s'.", propertyPath, value);
                assertThat(assertionReason, propertyValueFromResponse, containsString(value.trim()));
                logger.info(
                        "Response field value for {} is : {}."
                                + "\nValue expected to be contained in the specific path is: {}."
                                + "\nHence, actual property value contains expected value.",
                        propertyPath,
                        propertyValueFromResponse,
                        value);
            }
        } else {

            String assertionReason = String.format("Response field '%s' does not contain value '%s'.", propertyPath, containedValue);
            assertThat(assertionReason, propertyValueFromResponse, containsString(containedValue.toString()));
            logger.info(
                    "Response field value for {} is : {}."
                            + "\nValue expected to be contained in the specific path is: {}."
                            + "\nHence, actual property value contains expected value.",
                    propertyPath,
                    propertyValueFromResponse,
                    containedValue);
        }
    }

    public void propertyMatchesRegexValue(String propertyPath, Object regex) {

        String actualResponse = httpResponseManager.getResponse().asString();
        Object rawValue = com.jayway.jsonpath.JsonPath.read(actualResponse,"$." + propertyPath);
        if (rawValue == null || rawValue.toString().contains("[null]")) {
            return;
        }

        if (rawValue instanceof List) {
            List<?> propertyValueFromResponse = (List<?>) rawValue;

            if (regex.toString().contains(",")) {
                String[] valuesArray = regex.toString().split(",");
                for (int i = 0; i < propertyValueFromResponse.size(); i++) {
                    List<?> newList = (List<?>) propertyValueFromResponse.get(i);
                    for (int ite = 0; ite < valuesArray.length; ite++) {
                        String validate = String.valueOf(newList.get(ite));
                        String assertionReason = String.format(
                                        "Response field entity '%s' does not contain value matching the pattern %s.",
                                        propertyPath,
                                        valuesArray[ite]);

                        assertThat(assertionReason, validate, matchesPattern(valuesArray[ite]));

                        logger.info(
                                "Response field value for {} is : {}."
                                        + "\nPattern of value expected to be contained in the specific path is: {}."
                                        + "\nHence, actual property contains value matching the expected pattern.",
                                propertyPath,
                                propertyValueFromResponse,
                                valuesArray[ite]);
                    }
                }
            }

        } else if (rawValue instanceof String) {

            String propertyValue = (String) rawValue;
            String assertionReason = String.format(
                            "Response field '%s' does not contain value matching the pattern %s",
                            propertyPath,
                            regex.toString());
            assertThat(assertionReason, propertyValue, matchesPattern(regex.toString()));

            logger.info(
                    "Response field value for {} is : {}."
                            + "\nPattern of value expected to be contained in the specific path is: {}."
                            + "\nHence, actual property contains value matching the expected pattern.",
                    propertyPath,
                    propertyValue,
                    regex.toString());
        }
    }
}