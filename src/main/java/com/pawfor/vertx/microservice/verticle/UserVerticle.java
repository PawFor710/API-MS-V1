package com.pawfor.vertx.microservice.service;

import com.pawfor.vertx.microservice.domain.Item;
import com.pawfor.vertx.microservice.domain.User;
import com.pawfor.vertx.microservice.security.EncryptingPassword;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.List;
import java.util.UUID;

public class UserService extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private MongoClient client;
    private String token;
    private String userId;

    @Override
    public void start() throws Exception {
        LOGGER.info("Microservice app started");

        Router router = Router.router(vertx);

        //Users service
        router.route("/v1*").handler(BodyHandler.create());
        router.post("/v1/register").handler(this::registerUser);
        router.post("/v1/login").handler(this::loginUser);

        //Items service
        router.get("/v1/items").handler(this::getAllUserItems);
        router.route("/v1/items*").handler(BodyHandler.create());
        router.post("/v1/items").handler(this::addUserItem);


        JsonObject dbConfig = new JsonObject();
        dbConfig.put("connection_string", "mongodb://localhost:27017/MicroserviceRecruitment");
        dbConfig.put("useObjectId", false);

        client = MongoClient.createShared(vertx, dbConfig);

        vertx.createHttpServer().requestHandler(router).listen(3000);
    }


    //users service
    private void registerUser(RoutingContext routingContext)  {
        EncryptingPassword encrypt = new EncryptingPassword();
        JsonObject jsonBody = routingContext.getBodyAsJson();

        String login = jsonBody.getString("login");
        String password = jsonBody.getString("password");
        UUID id = UUID.randomUUID();

        String encryptedPassword = encrypt.encryptPassword(password);

        User newUser = new User(id, login, encryptedPassword);

        client.save("users", JsonObject.mapFrom(newUser), res ->{
            if (res.succeeded()) {
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(204)
                        .end(Json.encodePrettily(new JsonObject().put("Registration:", "Registering successfully")));
            } else {
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(400)
                        .end(Json.encodePrettily(new JsonObject().put("error", "user did not register")));
            }
        });
    }

    private void loginUser(RoutingContext routingContext)  {
        EncryptingPassword encrypt = new EncryptingPassword();

        final JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("login")));

        JsonObject jsonBody = routingContext.getBodyAsJson();

        String login = jsonBody.getString("login");
        String password = jsonBody.getString("password");
        String encryptedPassword = encrypt.encryptPassword(password);

        JsonObject body = new JsonObject();

        body.put("login", login);
        body.put("password", encryptedPassword);

        JsonObject fields = new JsonObject();
        fields.put("login", 0);
        fields.put("_id", 0);
        fields.put("password", 0);

        FindOptions options = new FindOptions();
        options.setFields(fields);

        client.findWithOptions("users", body, options , res -> {
            try {
                List<JsonObject> objects = res.result();
                if (objects != null && objects.size() != 0) {
                    token = provider.generateToken(new JsonObject());
                    routingContext.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json")
                            .end(Json.encodePrettily(new JsonObject().put("token", token)));

                    userId = Json.encode(objects.get(0)).substring(7,43);
                } else {
                    routingContext.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(400)
                            .end(Json.encodePrettily(new JsonObject().put("error", "Login or password was wrong, You are not logged")));
                }
            } catch (Exception e) {
                LOGGER.info("getAllUsers failed with exception " + e);

                routingContext.response()
                        .setStatusCode(500)
                        .putHeader("content-type", "application/json")
                        .end(Json.encodePrettily(new JsonObject().put("error", "internal server error")));
            }
        });
    }
    //Items service
    private void addUserItem(RoutingContext routingContext) {

        JsonObject jsonBody = routingContext.getBodyAsJson();

        String name = jsonBody.getString("name");

        UUID id = UUID.randomUUID();

        Item newItem = new Item(id, userId, name);

        client.save("items", JsonObject.mapFrom(newItem), res ->{
            try {
                String headerToken = routingContext.request().getHeader(HttpHeaders.AUTHORIZATION)
                        .substring("Bearer ".length());
                if (headerToken.equals(token)) {
                    routingContext.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(204)
                            .end(Json.encodePrettily(new JsonObject().put("Create", "Item created successfully")));

                } else {
                    routingContext.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(401)
                            .end(Json.encodePrettily(new JsonObject().put("error",
                                    "You have not provided an authentication token," +
                                    " the one provided has expired, was revoked or is not authentic")));
                }
            } catch (Exception e) {
                LOGGER.info("addUserItem failed with exception " + e);

                routingContext.response()
                        .setStatusCode(500)
                        .putHeader("content-type", "application/json")
                        .end(Json.encodePrettily(new JsonObject().put("error", "internal server error")));
            }
        });
    }

    private void getAllUserItems(RoutingContext routingContext)  {

        JsonObject ownerJson = new JsonObject();
        ownerJson.put("ownerId", userId);


        JsonObject fields = new JsonObject();
        fields.put("_id", 0);
        fields.put("ownerId", 0);

        FindOptions options = new FindOptions();
        options.setFields(fields);

        client.findWithOptions("items", ownerJson, options, res -> {
            try {
                List<JsonObject> objects = res.result();
                String headerToken = routingContext.request().getHeader(HttpHeaders.AUTHORIZATION)
                        .substring("Bearer ".length());
                if (headerToken.equals(token)) {
                    if (objects != null && objects.size() != 0) {
                        JsonObject jsonResponse = new JsonObject();
                        jsonResponse.put("items", objects);
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json")
                                .end(Json.encodePrettily(jsonResponse));
                    } else {
                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(400)
                                .end(Json.encodePrettily(new JsonObject().put("items", "You have no item")));
                    }
                } else {
                    routingContext.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(401)
                            .end(Json.encodePrettily(new JsonObject().put("error",
                                    "You have not provided an authentication token," +
                                    " the one provided has expired, was revoked or is not authentic")));
                }
            } catch (Exception e) {
                LOGGER.info("getAllItems failed with exception " + e);

                routingContext.response()
                        .setStatusCode(500)
                        .putHeader("content-type", "application/json")
                        .end(Json.encodePrettily(new JsonObject().put("error", "internal server error")));
            }
        });

    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Microservice stopped");
    }
}
