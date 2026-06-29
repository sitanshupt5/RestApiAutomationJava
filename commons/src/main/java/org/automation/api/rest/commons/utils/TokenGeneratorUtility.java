package org.automation.api.rest.commons.utils;

import org.automation.api.rest.commons.constants.ConfigConstants;
import org.automation.api.rest.commons.httpservicemanager.ConfigManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenGeneratorUtility {

    private static final Logger Logger =
            LoggerFactory.getLogger(TokenGeneratorUtility.class);

    public String getToken() {

        String clientId =
                System.getenv(ConfigConstants.AUTH_CLIENT_ID_ENV_VAR);

        String clientSecret =
                System.getenv(ConfigConstants.AUTH_CLIENT_SECRET_ENV_VAR);

        if (clientId == null || clientSecret == null) {
            clientId =
                    ConfigManager.getEnvProperty(ConfigConstants.AUTH_CLIENT_ID);

            clientSecret =
                    ConfigManager.getEnvProperty(ConfigConstants.AUTH_CLIENT_SECRET);
        }

        if (clientId == null || clientSecret == null) {

            String env = System.getProperty(
                    ConfigConstants.ENV_TYPE,
                    ConfigConstants.DEFAULT_ENV);

            throw new IllegalStateException(
                    "Test execution for environment '" + env +
                            "' is restricted to pipeline-only. " +
                            "If you are executing on pipeline please check the authorization credentials " +
                            "are correctly mapped to repository variables.");
        }

        return fetchBearerToken(
                clientId,
                clientSecret,
                resolveTokenUrl());
    }

    private String resolveTokenUrl() {

        String authUrl =
                ConfigManager.getEnvProperty(ConfigConstants.AUTH_URL);

        String partial =
                ConfigManager.getEnvProperty(
                        ConfigConstants.AUTH_URL_PARTIAL);

        if ("true".equalsIgnoreCase(partial)) {
            return authUrl + "/oauth2/token";
        }

        return authUrl;
    }

    private String fetchBearerToken(
            String clientId,
            String clientSecret,
            String tokenUrl) {

        String encodedCredentials = Base64.getEncoder()
                .encodeToString(
                        (clientId + ":" + clientSecret)
                                .getBytes(StandardCharsets.UTF_8));

        HttpClient httpClient =
                HttpClientBuilder.create().build();

        HttpPost httpPost = new HttpPost(tokenUrl);

        httpPost.setHeader(
                "Authorization",
                "Basic " + encodedCredentials);

        httpPost.setHeader(
                "Content-Type",
                "application/x-www-form-urlencoded");

        httpPost.setEntity(
                new StringEntity(
                        "grant_type=client_credentials&client_id=" + clientId,
                        StandardCharsets.UTF_8));

        String responseBody = null;

        try {
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            responseBody = EntityUtils.toString(entity);

        } catch (Exception e) {
            Logger.error(
                    "Error while generating access token from token endpoint: {}",
                    e.getMessage());
        }

        JSONObject jsonObject = new JSONObject(responseBody);

        return jsonObject.getString("access_token");
    }
}
