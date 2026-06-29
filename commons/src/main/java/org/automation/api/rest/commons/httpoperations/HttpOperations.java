package org.automation.api.rest.commons.httpoperations;

import org.automation.api.rest.commons.customexceptions.CustomException;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public enum HttpOperations {

    GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH;

    public static HttpOperations parse(String apiRequest) {
        for (HttpOperations httpMethod: values()) {
            if(httpMethod.toString().equalsIgnoreCase(apiRequest))
                return httpMethod;
        }
        throw new IllegalArgumentException();
    }

    public Response doRequest(RequestSpecification when, String url) {

        switch (this) {

            case GET: {
                when.basePath(url);
                when.log().all(true);
                return when.get();
            }

            case POST: {
                when.basePath(url);
                when.log().all(true);
                return when.post();
            }

            case PUT: {
                when.basePath(url);
                when.log().all(true);
                return when.put();
            }

            case DELETE: {
                when.basePath(url);
                when.log().all(true);
                return when.delete();
            }

            case OPTIONS: {
                when.basePath(url);
                when.log().all(true);
                return when.options();
            }

            case HEAD: {
                when.basePath(url);
                when.log().all(true);
                return when.head();
            }

            case PATCH: {
                when.basePath(url);
                when.log().all(true);
                return when.patch();
            }
        }

        throw new CustomException(
                "The given http method: " + url + "is not valid.");
    }
}
