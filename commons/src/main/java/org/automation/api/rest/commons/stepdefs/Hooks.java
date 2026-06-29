package org.automation.api.rest.commons.stepdefs;

import static com.github.automatedowl.tools.AllureEnvironmentWriter.allureEnvironmentWriter;
import com.google.common.collect.ImmutableMap;
import org.automation.api.rest.commons.httpservicemanager.ConfigManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.cucumber.messages.internal.com.google.common.collect.ImmutableMap.builder;

/**
 * @author sitanshu pati
 */
public class Hooks {

    private static final Logger Logger =
            LoggerFactory.getLogger(Hooks.class);

    private TestManagerContext testManagerContext;
    String logFolder = "apilogs";

    public Hooks(TestManagerContext context) {
        this.testManagerContext = context;
    }

    public Hooks() {
    }

    @Before()
    public void beforeScenario(Scenario scenario) throws Exception {

        if (System.getProperty("AllureEnv") == null) {

            allureEnvironmentWriter(
                    ImmutableMap.<String, String>builder()
                            .put("Environment",
                                    ConfigManager.getSystemPropertyOrSetDefault(
                                            "env.type", "qaenv"))
                            .put("User",
                                    ConfigManager.getSystemPropertyOrSetDefault(
                                            "user.name", "Automation"))
                            .put("project",
                                    String.valueOf(Paths.get("")
                                            .toAbsolutePath()
                                            .getParent()))
                            .build());

            System.setProperty("AllureEnv", "TRUE");
        }

        Logger.info("Report file path setup completed.");
        Logger.info("### SCENARIO: {}", scenario.getName());

        ConfigManager.getSystemPropertyOrSetDefault(
                "logs.extract", "false");

        if (System.getProperty("logs.extract").equals("true")) {

            PrintStream printStream = null;
            File fileWriter;

            String scenarioID = scenario.getId();
            String[] fileName = scenarioID.split("[_;:/]");

            String dir = logFolder + "/"
                    + fileName[12] + "/"
                    + fileName[13];

            File featureDirectory = new File(dir);

            if (!featureDirectory.exists()) {
                featureDirectory.mkdirs();
            }

            String scName = scenario.getName()
                    .split(":")[1]
                    .trim()
                    .split("\\.")[1];

            fileWriter = new File(
                    dir + "/" + scName + ".log");

            if (!fileWriter.exists()) {
                try {
                    fileWriter.createNewFile();
                } catch (IOException e) {
                    System.out.println(
                            "file not created:"
                                    + fileWriter.getPath());
                }
            }

            Logger.info("Log file path setup completed.");

            try {

                printStream = new PrintStream(
                        new FileOutputStream(fileWriter),
                        true);

                printStream.append(
                        "Scenario Name: "
                                + scenario.getName()
                                + System.lineSeparator());

                testManagerContext
                        .getHttpRequest()
                        .restConfig
                        .setDefaultStream(printStream);

                Logger.info(
                        "Initial request specification setup completed. " +
                                "More specs to be added further.");

            } catch (FileNotFoundException e) {
                throw new FileNotFoundException(
                        "file not found");
            }
        }

        if (!scenario.getId().contains("manager_category")
                && !scenario.getId().contains("dei")
                && !scenario.getId().contains("position")) {

            testManagerContext
                    .getHttpRequest()
                    .initNewSpecification();
        }
    }

    @After()
    public void afterScenario(Scenario scenario)
            throws Exception {

        if (System.getProperty("logs.extract")
                .equals("true")) {

            String scenarioID = scenario.getId();
            String[] fileName =
                    scenarioID.split("[_;:/]");

            String scName = scenario.getName()
                    .split(":")[1]
                    .trim()
                    .split("\\.")[1];

            File log = new File(
                    logFolder + "/"
                            + fileName[12]
                            + "/"
                            + fileName[13]
                            + "/"
                            + scName
                            + ".log");

            byte[] byteData = new byte[0];

            try {
                byteData = Files.readAllBytes(
                        log.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            scenario.attach(
                    byteData,
                    "text/plain",
                    scName + ".log");
        }

        try {

            Logger.info(
                    "Moving environment file to allure-results folder");

            if (Files.exists(Paths.get( "./target/allure-results/environment.xml"))) {

                Path temp = Files.move(
                        Paths.get( "./target/allure-results/environment.xml"),
                        Paths.get("./build/allure-results/environment.xml"));

                Logger.info(
                        "Moved environment file to allure-results folder");
            }

            Logger.info(
                    "Moving old history folder to allure-results");

            if (Files.exists(Paths.get("history"))) {

                Path temp = Files.move(
                        Paths.get("history"),
                        Paths.get(
                                "./build/allure-results/history"));

                Logger.info(
                        "Moved old history folder to allure-results");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        testManagerContext
                .getSoftAssertions()
                .assertAll();
    }
}