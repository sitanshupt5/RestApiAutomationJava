package org.automation.api.rest.commons.stepdefs;

import org.automation.api.rest.commons.enums.ApiContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sitanshu pati
 */
public class TestScenarioContext {
    private Map<String, Object> scenarioContext;

    public TestScenarioContext() {
        scenarioContext = new HashMap<>();
    }

    public void setContext(ApiContext key, Object value) {
        scenarioContext.put(key.toString(), value);
    }

    public Object getContext(ApiContext key) {
        return scenarioContext.get(key.toString());
    }

    public Boolean isContains(ApiContext key) {return scenarioContext.containsKey(key.toString());}
}
