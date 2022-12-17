package com.pawfor.vertx.microservice.verticle;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.restassured.specification.RequestSpecification;


import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;


@ExtendWith(VertxExtension.class)
class UserServiceTest {
    private static RequestSpecification requestSpecification;

    @BeforeAll
    static void prepareSpec() {
        requestSpecification = new RequestSpecBuilder()
                .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
                .setBaseUri("http://localhost:3000/")
                .build();
    }

    @BeforeEach
    void setup(Vertx vertx, VertxTestContext testContext) {
        vertx
                .deployVerticle(new UserVerticle(),
                        res -> {
                    if (res.succeeded()) {
                        testContext.completeNow();
                    } else {
                        testContext.failNow(res.cause());
                    }
                });
    }

    @Test
    @DisplayName("register test")
    void registrationTest() {
        String login = "test@login";
        String password = "password";
        JsonObject body = new JsonObject();
        body.put("login", login);
        body.put("password", password);

        given(requestSpecification)
                .contentType(ContentType.JSON)
                .body(body.encode())
                .post("/v1/register")
                .then()
                .assertThat()
                .statusCode(204);

    }

    @Test
    @DisplayName("login test")
    void loginTest() {
        String login = "test@login";
        String password = "password";
        JsonObject body = new JsonObject();
        body.put("login", login);
        body.put("password", password);

        given(requestSpecification)
                .contentType(ContentType.JSON)
                .body(body.encode())
                .post("/v1/login")
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("addItem test")
    void addItemTest() {

        requestSpecification = new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer 1234")
                .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
                .setBaseUri("http://localhost:3000/")
                .build();


        JsonObject body = new JsonObject();
        body.put("name", "item");

        given(requestSpecification)
                .contentType(ContentType.JSON)
                .body(body.encode())
                .post("/v1/items")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    @DisplayName("getAllUserItem test")
    void getAllItemTest() {

        requestSpecification = new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer 1234")
                .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
                .setBaseUri("http://localhost:3000/")
                .build();

        given(requestSpecification)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer 1234")
                .get("/v1/items")
                .then()
                .assertThat()
                .statusCode(401);
    }
}