package org.automation.api.rest.commons.stepdefs;

import com.google.common.base.Strings;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.restassured.http.ContentType;
import org.automation.api.rest.commons.constants.Entity;
import org.automation.api.rest.commons.constants.Headers;
import org.automation.api.rest.commons.enums.ApiContext;
import org.automation.api.rest.commons.httpservicemanager.RestRequestManager;
import org.automation.api.rest.commons.utils.ApiUtilManager;
import org.automation.api.rest.commons.stepdefs.TestManagerContext;
import org.automation.api.rest.commons.utils.TokenGeneratorUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public class CommonGivenTestSteps {

    private static final Logger logger = LoggerFactory.getLogger(CommonGivenTestSteps.class);

    public RestRequestManager restRequestManager;
    TestManagerContext testManagerContext;

    public CommonGivenTestSteps(TestManagerContext context) {
        testManagerContext = context;
        restRequestManager = testManagerContext.getRestRequest();
    }

    @Given("I have authorization token")
    public void i_have_authorization_token() {
        TokenGeneratorUtility tokenGeneratorUtility = new TokenGeneratorUtility();
        String accessToken = null;

        try {
            accessToken = tokenGeneratorUtility.getToken();
        } catch (Exception e) {
            logger.info("Error while generating token " + e);
        }

        testManagerContext.getScenarioContext().setContext(ApiContext.ACCESS_TOKEN, accessToken);
    }

    @Given("I have API {string}")
    public void iHaveAPI(String apiName) throws IOException, URISyntaxException {
        testManagerContext.getScenarioContext().setContext(ApiContext.API_NAME, apiName);

        logger.info("Api name set in scenarios context as: "
                + testManagerContext.getScenarioContext().getContext(ApiContext.API_NAME));

        ApiUtilManager apiUtilManager = new ApiUtilManager();

        String basePath = apiUtilManager.getBasePath(
                (String) testManagerContext.getScenarioContext().getContext(ApiContext.API_NAME));

        testManagerContext.getScenarioContext().setContext(ApiContext.BASE_PATH, basePath);
        logger.info("Base path setup in scenario context as: " + basePath);

        apiUtilManager.setEntityHostURI(apiName, testManagerContext);

        restRequestManager.clearRequestBody();
        logger.info("Existing request body has been cleared");
    }

    @And("^I set content-type as (.+)$")
    public void content_Type(String contentType) {
        restRequestManager.contentType(ContentType.valueOf(contentType).withCharset("utf-8"));
        logger.info("Content type has been setup as: " + contentType);
    }

    @And("I set request body for {string}")
    public void iSetRequestBodyAs(String customer) throws IOException, URISyntaxException, ParseException {
        ApiUtilManager apiUtilManager = new ApiUtilManager();

        restRequestManager.clearRequestQueryParam();

        iSetRequestHeader();
        iSetQueryParams(customer);

        if (restRequestManager.getQueryparam().isEmpty() && !customer.contains("QueryParams")) {
            restRequestManager.setRequestBody(apiUtilManager.getRequestBody(testManagerContext, customer));

            testManagerContext.getScenarioContext()
                    .setContext(ApiContext.REQUEST_BODY, restRequestManager.getRequestBody());

            logger.info("Request body is set in scenario context.");
        }
    }

    private void iSetQueryParams(String customer) throws IOException, URISyntaxException, ParseException {
        ApiUtilManager apiUtilManager = new ApiUtilManager();

        restRequestManager.clearRequestQueryParam();
        restRequestManager.setQueryparam(apiUtilManager.getQueryParams(testManagerContext, customer));
    }

    @Given("I set header {string} with a value of {string}")
    public void iProvideTheHeaderWithAValueOf(String name, String value) {
        restRequestManager.setRequestHeader(name, value);

        testManagerContext.getScenarioContext().setContext(
                ApiContext.BRAND_HEADER,
                restRequestManager.getHeader().get(Headers.COMMON_HEADERS));
    }

    @And("I set request header")
    public void iSetRequestHeader() throws IOException, URISyntaxException {
        ApiUtilManager apiUtilManager = new ApiUtilManager();

        restRequestManager.setRequestHeader(apiUtilManager.getHeader(
                (String) testManagerContext.getScenarioContext().getContext(ApiContext.API_NAME),
                (String) testManagerContext.getScenarioContext().getContext(ApiContext.ID)));

        String accessToken = (String) testManagerContext.getScenarioContext().getContext(ApiContext.ACCESS_TOKEN);
        String apiName = (String) testManagerContext.getScenarioContext().getContext(ApiContext.API_NAME);
        Boolean isAuthAPI = apiName.toLowerCase().contains(Entity.AUTH_API);

        if (!Strings.isNullOrEmpty(accessToken) && restRequestManager.getHeader().containsKey("Authorization")) {
            restRequestManager.setRequestHeader("Authorization", "Bearer " + accessToken);
        }
    }

    @And("I set parameter {string} with a value of {string}")
    public void iSetParameterWithAValueOf(String name, String value) {
        restRequestManager.setRequestParam(name, value);
    }

    @And("I set the new correlationId in header")
    public void iSetTheNewCorrelationIdInHeader() {
        ApiUtilManager apiUtilManager = new ApiUtilManager();
        restRequestManager.setRequestHeader("x-txn-correlation-id", apiUtilManager.getUniqueCorrelationid());
    }

    @Given("I setup request to generate Auth Token")
    public void iSetupRequestToGenerateAuthToken() throws IOException, URISyntaxException, ParseException {
        iSetAuthCredentials((String) testManagerContext.getScenarioContext().getContext(ApiContext.API_NAME));
        iSetRequestBodyAs(null);
    }

    @Given("I set authentication credentials")
    private void iSetAuthCredentials(String api) throws IOException, URISyntaxException {
        ApiUtilManager apiUtilManager = new ApiUtilManager();
        restRequestManager.setAuthCredentials(apiUtilManager.getAuthParams(api));
    }
}