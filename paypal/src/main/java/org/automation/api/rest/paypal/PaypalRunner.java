package org.automation.api.rest.paypal;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"src/main/resources/features"},
        glue = {"org.automation.api.rest.commons.stepdefs"},
        tags = "@Smoke",
        plugin = {
                "pretty",
                "html:target/cucumber/cucumber-report.html",
                "json:target/cucumber_json",
                "io.qameta.allure.cucumber6jvm.AllureCucumber6Jvm",
                "org.automation.api.rest.commons.listeners.CucumberListener"
        }
)
public class PaypalRunner {

}
