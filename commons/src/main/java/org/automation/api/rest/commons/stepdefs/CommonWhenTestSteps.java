package org.automation.api.rest.commons.stepdefs;

import org.automation.api.rest.commons.httpservicemanager.HttpResponseManager;
import org.automation.api.rest.commons.httpservicemanager.RestRequestManager;
import org.automation.api.rest.commons.enums.ApiContext;
import org.automation.api.rest.commons.utils.ApiUtilManager;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CommonWhenTestSteps {

    private static final Logger logger = LoggerFactory.getLogger(CommonWhenTestSteps.class);
    HttpResponseManager httpResponseManager;
    TestManagerContext testManagerContext;
    RestRequestManager restRequestManager;

    public CommonWhenTestSteps(TestManagerContext context) {
        testManagerContext = context;
        httpResponseManager = testManagerContext.getHttpResponse();
        restRequestManager = testManagerContext.getRestRequest();
    }

    @When("^the client performs (.+) request on API \\\"(.+)\\\"$")
    public void perform_Http_Request(String httpMethod, String url) throws Throwable {
        httpResponseManager.setResponsePrefix("");
        ApiUtilManager apiUtilManager = new ApiUtilManager();
        httpResponseManager.setResponse(httpResponseManager.doRequest(httpMethod, apiUtilManager.getBasePath(url)));
    }

    @When("I call method {string}")
    public void iCallMethodPOST(String httpMethod) throws Exception {
        httpResponseManager.setResponsePrefix("");
        String basePath = (String) testManagerContext.getScenarioContext().getContext(ApiContext.BASE_PATH);
        httpResponseManager.setResponse(httpResponseManager.doRequest(httpMethod, basePath));
        iGetTheResponse();
    }

    @And("I get the response")
    public void iGetTheResponse() {
        testManagerContext.getScenarioContext().setContext(ApiContext.RESPONSE_BODY, httpResponseManager.getResponse().asString());
    }

    @And("I save the initial response")
    public void iSaveTheInitialResponse() {
        testManagerContext.getScenarioContext().setContext(ApiContext.INITIAL_RESPONSE_BODY, httpResponseManager.getResponse().asString());
    }
}