package org.automation.api.rest.commons.httpservicemanager;

import org.automation.api.rest.commons.constants.ConfigConstants;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.path.json.config.JsonPathConfig;

import java.io.PrintStream;

import static io.restassured.config.RestAssuredConfig.newConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.DecoderConfig.ContentDecoder.DEFLATE;
import static io.restassured.config.DecoderConfig.decoderConfig;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.LogConfig.logConfig;

/**
 * @author sitanshu pati
 */
public class RestServiceConfigManager {
    private ConfigManager world;

    private RestAssuredConfig restAssuredConfig;
    private PrintStream logstream;

    public RestServiceConfigManager(ConfigManager configManager) {
        this.world = configManager;
    }

    public static com.github.dzieciou.testing.curl.Options getCurlOptions() {
        return com.github.dzieciou.testing.curl.Options.builder()
                .updateCurl(curl -> curl.setInsecure(false)
                .removeHeader("Host")
                .removeHeader("User-Agent")
                .removeHeader("Connection"))
                .printMultiliner()
                .useLongForm()
                .build();
    }

    public RestAssuredConfig getConfig() {
        if (restAssuredConfig == null){
            restAssuredConfig = buildResAssuredConfig();
        }
        return restAssuredConfig;
    }

    private RestAssuredConfig buildResAssuredConfig() {
        RestAssuredConfig config = newConfig()
                .jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL))
                .redirect(redirectConfig().followRedirects(world.getBoolean(ConfigConstants.FOLLOW_REDIRECTS)))
                .sslConfig(new SSLConfig().allowAllHostnames());

        if (logstream!= null)
            config = config.logConfig(logConfig().defaultStream(logstream));
        if (!world.getBoolean(ConfigConstants.GZIP_SUPPORT, true)) {
            config = config.decoderConfig(decoderConfig().contentDecoders(DEFLATE));
        }

        return config;
    }

    public void setDefaultStream(PrintStream logstream)
    {
        this.logstream = logstream;
    }

    public void reset() { restAssuredConfig = null;}
}
