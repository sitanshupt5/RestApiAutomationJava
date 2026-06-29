package org.automation.api.rest.commons.utils;

import org.automation.api.rest.commons.customexceptions.ContentNotFoundException;
import org.automation.api.rest.commons.customexceptions.InvalidFileFormatException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    private final static Logger logger = LogManager.getLogger(FileUtil.class);
    private static final String NL = System.getProperty("line.separator");
    private static final String FS = File.separator;

    public static String readContentFromFile(String filepath) throws IOException, InvalidFileFormatException, ContentNotFoundException {
        checkFileExtension(filepath);
        String fileContent = new String(Files.readAllBytes(Paths.get(filepath)), "UTF-8");
        if (fileContent.isEmpty()){
            throw new ContentNotFoundException("No content found in the expected file "+FilenameUtils.getName(filepath));
        }
        return fileContent;
    }

    private static void checkFileExtension(String filepath) throws FileNotFoundException, InvalidFileFormatException {
        checkFileExists(filepath);
        String fileExtensionName = FilenameUtils.getExtension(filepath);
        switch (fileExtensionName){
            case "json": {
                logger.info("file extension is json");
                break;
            }
            case "txt" : {
                logger.info("file extension is text");
                break;
            }
            case "xml" : {
                logger.info("file extension is xml");
                break;
            }
            default: {
                logger.info("File extension should be either .json, .txt or .xml, but the actual format is {}", fileExtensionName);
                throw new InvalidFileFormatException("Invalid file format");
            }

        }
    }

    private static void checkFileExists(String filepath) throws FileNotFoundException {
        if(!Files.exists(Paths.get(filepath))){
            throw new FileNotFoundException("File "+ FilenameUtils.getName(filepath)+ " not found.");
        }
    }

    public static List<String> readAllLinesFromFile(String filePath) throws IOException {
        checkFileExists(filePath);
        return Files.readAllLines(Paths.get(filePath));
    }

    public static void writeToFile(String directoryName, String inputFieldName,
                                   String content) throws IOException {
        createDirectoryIfNotExists(directoryName);
        File actualFile = createFileIfNotExists(directoryName+FS, inputFieldName);
        if (actualFile.exists()) {
            FileWriter fileWriter = new FileWriter(actualFile.getAbsoluteFile(), false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
            bufferedWriter.close();
        }

    }

    private static void createDirectoryIfNotExists(String directoryName) throws IOException {
        File directory =  new File(directoryName);
        if (!directory.exists()){
            boolean success = directory.mkdirs();
            if (!success) {
                throw new IOException("Directory: "+directoryName+" not created");
            }
        }
    }

    private static File createFileIfNotExists(String directoryName, String fileName) throws IOException {

        if (directoryName==null){
            throw new IOException("Directory name should not be null.");
        }
        if (fileName==null){
            throw new IOException("File name should not be null.");
        }

        File tempFile = new File(directoryName+FS+fileName);
        if (!tempFile.exists()){
            try {
                tempFile.createNewFile();
            }catch (IOException e)
            {
                logger.error("Unable to create the file: {} in the driectory: {}"+NL+e, fileName, directoryName);
                throw  e;
            }
        }else if (tempFile.exists()) {
            PrintWriter writer = new PrintWriter(tempFile);
            writer.print("");
            writer.close();
        }
        return tempFile;
    }

    /*
    * Method to delete the file
    *
    * @param fileName
    *            name of the file to delete.
    */
    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        try{
            if (file.exists()){
                file.delete();
            }
        }catch (Exception e)
        {
            logger.error("Unable to delete the file: {}", fileName);
            throw e;
        }
    }

    /*
    * Method to read content from the file.
    *
    * @param fileName
    *            name of the file.
    *
    * @return the content in the String.
    * @throws IOException
    *               if unable to read the file.
    * */
    public static String getContentFromTextFile(String fileName) throws IOException{
        Class clazz = FileUtil.class;
        InputStream inputStream = clazz.getResourceAsStream(fileName);
        return readFromInputStream(inputStream);
    }

    private static String readFromInputStream(InputStream inputStream) {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return resultStringBuilder.toString();
    }

    static List<String> readLines(File file) throws IOException{
        return readLines(file, Charset.defaultCharset());
    }

    private static List<String> readLines(File file, Charset encoding) throws IOException {
        FileInputStream in = null;
        List<String> var3;
        try {
            in = openInputStream(file);
            var3 = readLines(in,encoding==null ? Charset.defaultCharset() : encoding);
        }finally {
            closeQuietly(in);
        }
        return var3;
    }


    static byte[] readBytes(File file) throws IOException {
        return IOUtils.toByteArray(new FileInputStream(file));
    }


    private static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()){
            if (file.isDirectory()) {
                throw  new IOException("File \'"+file+"\' exists but is a directory");
            }else if (!file.canRead())
            {
                throw  new IOException("File \'"+file+"\' cannot be read");
            }
            else{
                return new FileInputStream(file);
            }
        }else{
            throw  new IOException("File \'"+file+"\' does not exist.");
        }
    }

    public static boolean isSeparator(char ch) {return ch==47||ch==92;}

    public static URL createCukesPropertyFileUrl(final ClassLoader classLoader) {
        String cukesProfile = System.getProperty("cukes.profile");

        String propertiesFileName = cukesProfile ==null ||cukesProfile.isEmpty()
                ? "cukes.properties"
                : "cukes-" +cukesProfile + ".properties";
        return classLoader.getResource(propertiesFileName);
    }


    private static void closeQuietly(FileInputStream closeable) {
        try{
            if(closeable != null) {
                closeable.close();
            }
        }catch(IOException ignored) {}
    }

    private static List<String> readLines(InputStream inputStream, Charset encoding) throws IOException {
        InputStreamReader input = new InputStreamReader(inputStream, encoding==null?Charset.defaultCharset():encoding);
        BufferedReader reader = toBufferedReader(input);
        List<String> list = new ArrayList<>();

        for(String line =reader.readLine(); line!=null; line = reader.readLine()){
            list.add(line);
        }
        return list;
    }

    private static BufferedReader toBufferedReader(Reader reader) {
        return reader instanceof BufferedReader?(BufferedReader)reader:new BufferedReader(reader);
    }

}
