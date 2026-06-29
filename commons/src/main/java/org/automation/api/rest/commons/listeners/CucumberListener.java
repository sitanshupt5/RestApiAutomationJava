package org.automation.api.rest.commons.listeners;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunStarted;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class CucumberListener implements EventListener {

    Logger logger = LoggerFactory.getLogger(CucumberListener.class);

    @Override
    public void setEventPublisher(EventPublisher publisher) {

        logger.info("Deleting existing files from allure-results folder using Event Listener");

        publisher.registerHandlerFor(TestRunStarted.class,
                (TestRunStarted event) -> {
                    try {
                        logger.info("Deleting existing files from allure-results");
                        File allureResult =
                                new File("./build/allure-results");
                        deleteDirectory(allureResult);

                        logger.info("Deleting existing files from allure-reports");
                        File allureReport =
                                new File("./build/reports/allure-report");
                        deleteDirectory(allureReport);

                    } catch (Exception e) {
                        logger.info(
                                "Filepath/Directory is already empty. Hence, skipping cleanup");
                    }
                });
    }

    public static void deleteDirectory(File file) {

        for (File subfile : file.listFiles()) {

            if (subfile.isDirectory()) {
                deleteDirectory(subfile);
            }

            subfile.delete();
        }
    }
}
