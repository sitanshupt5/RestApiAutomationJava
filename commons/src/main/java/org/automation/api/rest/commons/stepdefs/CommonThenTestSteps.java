package org.automation.api.rest.commons.stepdefs;

import com.google.common.base.Strings;
import com.jayway.jsonpath.JsonPath;
import org.automation.api.rest.commons.httpservicemanager.HttpServiceAssertion;
import org.automation.api.rest.commons.httpservicemanager.HttpRequestManager;
import org.automation.api.rest.commons.httpservicemanager.RestRequestManager;
import org.automation.api.rest.commons.enums.ApiContext;
import org.automation.api.rest.commons.utils.ApiUtilManager;
import org.automation.api.rest.commons.utils.JsonUtil;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class CommonThenTestSteps {

    HttpServiceAssertion httpServiceAssertion;
    TestManagerContext testManagerContext;
    RestRequestManager restRequestManager;
    HttpRequestManager httpRequestManager;
    ApiUtilManager apiUtilManager;

    private static final Logger logger =
            LoggerFactory.getLogger(CommonThenTestSteps.class);

    public CommonThenTestSteps(TestManagerContext context) {
        testManagerContext = context;
        this.httpServiceAssertion =
                new HttpServiceAssertion(testManagerContext.getHttpResponse());
        restRequestManager = testManagerContext.getRestRequest();
        httpRequestManager = testManagerContext.getHttpRequest();
        apiUtilManager = new ApiUtilManager();
    }

    @Then("I verify response code is {int}")
    public void iVerifyResponseCodeIs(int statusCode) {
        httpServiceAssertion.statusCodeIs(statusCode);
    }

    @And("I verify fields in response")
    public void iVerifyFieldsInResponse(DataTable table) {

        table
                .asMap(String.class, String.class)
                .entrySet()
                .stream()
                .skip(1)
                .forEach(
                        (Map.Entry<Object, Object> entry) -> {
                            httpServiceAssertion.bodyContainsPropertyWithValue(
                                    (String) entry.getKey(),
                                    JsonUtil.getNodeValue(
                                            testManagerContext
                                                    .getScenarioContext()
                                                    .getContext(ApiContext.REQUEST_BODY)
                                                    .toString(),
                                            (String) entry.getValue()));
                        });
    }

    @And("I verify {string} in Response")
    public void iVerifyResponse(String customer) {

        String request = (String) testManagerContext
                .getScenarioContext()
                .getContext(ApiContext.REQUEST_BODY);

        Map<String, Object> map =
                apiUtilManager.getSchema(
                        customer,
                        (String) testManagerContext
                                .getScenarioContext()
                                .getContext(ApiContext.API_NAME));

        map.forEach((String key, Object value) -> {

            httpServiceAssertion.bodyContainsPropertyWithValue(
                    key,
                    JsonUtil.getNodeValue(request, (String) value));
        });
    }

    @And("I verify attribute values match {string} in Response")
    public void iVerifyAttributeValuesMatchResponse(String customer)
            throws IOException, URISyntaxException {

        JSONObject validationData =
                apiUtilManager.readDataFile(
                        testManagerContext,
                        customer,
                        (String) testManagerContext
                                .getScenarioContext()
                                .getContext(ApiContext.API_NAME));

        Map<String, Object> map =
                apiUtilManager.getSchema(
                        customer,
                        (String) testManagerContext
                                .getScenarioContext()
                                .getContext(ApiContext.API_NAME));

        map.forEach((String key, Object value) -> {

            if (key.toLowerCase().contains("contains")) {

                httpServiceAssertion.propertyContainsValue(
                        (String) value,
                        validationData.get(key));

            } else if (key.toLowerCase().contains("regex")) {

                httpServiceAssertion.propertyMatchesRegexValue(
                        (String) value,
                        validationData.get(key));

            } else {

                httpServiceAssertion.bodyContainsPropertyWithExpectedValue(
                        (String) value,
                        validationData.get(key));
            }
        });
    }

    @And("I clear the request body")
    public void iClearTheRequestBody() {
        restRequestManager.clearRequestBody();
        httpRequestManager.body("");
    }

    @And("I clear the request headers")
    public void iClearTheRequestHeaders() throws IOException {
        restRequestManager.clearRequestHeader();
    }

    @And("I clear the query parameters")
    public void iClearTheQueryParameters() {
        restRequestManager.clearRequestParam();
    }

    @Then("I verify attributes in response using {string} for {string}")
    public void i_verify_attributes_in_response(
            String validationData,
            String api,
            DataTable dataTable)
            throws IOException, URISyntaxException {

        Map<String, String> attributes =
                dataTable.asMap(String.class, String.class);

        if (!Strings.isNullOrEmpty(validationData)) {

            JSONObject expectedResults =
                    apiUtilManager.readDataFile(
                            testManagerContext,
                            validationData,
                            api);

            for (Map.Entry<String, String> attribute :
                    attributes.entrySet()) {

                if (!expectedResults.get(attribute.getKey()).equals(null)) {

                    String expectedResult =
                            expectedResults.getString(attribute.getKey());

                    httpServiceAssertion.bodyContainsPropertyWithValue(
                            attribute.getValue(),
                            expectedResult);

                } else {

                    httpServiceAssertion.bodyContainsPropertyWithValue(
                            attribute.getValue(),
                            null);
                }
            }
        }
    }

    @Then("I verify response code is {string}")
    public void iVerifyResponseCodeIs(String statusCode) {
        httpServiceAssertion.statusCodeIs(
                Integer.valueOf(statusCode));
    }

    @Then("I save the access token")
    public void iSaveTheAccessToken() {

        String response =
                testManagerContext.getScenarioContext()
                        .getContext(ApiContext.RESPONSE_BODY)
                        .toString();

        String accessToken =
                JsonPath.read(response, "$.access_token").toString();

        testManagerContext.getScenarioContext()
                .setContext(ApiContext.ACCESS_TOKEN, accessToken);
    }
}