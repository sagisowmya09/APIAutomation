package org.example.restfulbooker.validation;

import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;

/**
 * Reusable response assertions for endpoint tests.
 */
public final class ResponseValidator {

    private ResponseValidator() {
    }

    public static void statusCode(Response response, int expectedStatusCode) {
        response.then().statusCode(expectedStatusCode);
    }

    public static void fieldEquals(Response response, String jsonPath, Object expectedValue) {
        response.then().body(jsonPath, equalTo(expectedValue));
    }

    public static void fieldIsUsable(Response response, String jsonPath) {
        String value = response.jsonPath().getString(jsonPath);
        assertThat(jsonPath + " must contain a value", value, not(blankOrNullString()));
    }
}
