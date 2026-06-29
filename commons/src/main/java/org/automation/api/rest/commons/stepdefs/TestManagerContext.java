package org.automation.api.rest.commons.stepdefs;

import org.automation.api.rest.commons.constants.ConfigConstants;
import org.automation.api.rest.commons.httpservicemanager.*;
import org.assertj.core.api.SoftAssertions;


/**
 * @author sitanshu pati
 */
public class TestManagerContext {
    private HttpRequestManager httpRequestManager;
    private HttpServiceAssertion httpServiceAssertion;
    private HttpResponseManager httpResponseManager;
    private RestRequestManager restRequestManager;
    private TestScenarioContext testScenarioContext;
    public ConfigManager configManager;
    private SoftAssertions softAssertions;


    public TestManagerContext() {
        configManager = new ConfigManager();
        configManager.initEnvProperties(getEnvProperty());
        httpRequestManager = new HttpRequestManager(configManager);
        restRequestManager = new RestRequestManager(httpRequestManager);
        httpResponseManager = new HttpResponseManager(configManager, httpRequestManager, restRequestManager);
        httpServiceAssertion = new HttpServiceAssertion(httpResponseManager);
        testScenarioContext = new TestScenarioContext();
        softAssertions = new SoftAssertions();
    }

    private String getEnvProperty() {
        return ConfigManager.getSystemPropertyOrSetDefault("env.type", ConfigConstants.DEFAULT_ENV);
    }

    public HttpRequestManager getHttpRequest() {
        return httpRequestManager;
    }

    public HttpServiceAssertion getHttpAssertion() {
        return httpServiceAssertion;
    }

    public HttpResponseManager getHttpResponse() {
        return httpResponseManager;
    }

    public RestRequestManager getRestRequest() {
        return restRequestManager;
    }

    public TestScenarioContext getScenarioContext() {
        return testScenarioContext;
    }

    public SoftAssertions getSoftAssertions() {
        return softAssertions;
    }
}
