package org.automation.api.rest.commons.utils;

import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ZipUtility {

    private static final Path REPORT_ZIP = Paths.get("report.zip");
    private static final String REPORT_DIR = "./build/reports/allure-report/";

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(ZipUtility.class);
        String folder = REPORT_DIR + "allureReport";

        try {
            if (Files.exists(Paths.get(folder))) {
                writeSupportFiles();

                try (ZipFile zipFile = new ZipFile(REPORT_ZIP.toFile())) {
                    zipFile.addFolder(new File(REPORT_DIR));
                }

                logger.info("Zipping of the report Folder completed");

                if (Files.exists(REPORT_ZIP)) {
                    Files.move(REPORT_ZIP,
                            Paths.get(REPORT_DIR + "report.zip"));
                }
            }
        } catch (Exception e) {
            logger.info("Error Occurred During Zipping", e);
        }
    }

    private static void writeSupportFiles() throws Exception {

        try (FileWriter batWriter =
                     new FileWriter(REPORT_DIR + "open_report.bat")) {

            batWriter.write("allure open allureReport");
        }

        try (FileWriter readMeWriter =
                     new FileWriter(REPORT_DIR + "README.txt")) {

            readMeWriter.write(
                    "To Open Allure Report run the open_report.bat file.\n" +
                            "\n" +
                            "If it shows allure is not recognized as an internal or external command then Allure CommandLine need to be setup in system.\n" +
                            "\n" +
                            "Link to download Allure CommandLine: https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline/2.19.0/allure-commandline-2.19.0.zip\n" +
                            "\n" +
                            "* After Download extract the zip file.\n" +
                            "* Set bin path into environment Variable.\n" +
                            "\n" +
                            "Now run open_report.bat file it will open report on default web browser."
            );
        }
    }
}
