package com.pawfor.vertx.microservice.authorization;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class Authorization extends AbstractVerticle {

    public String createToken() {

        final JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer("login")));

        return provider.generateToken(new JsonObject(), new JWTOptions().setExpiresInMinutes(10));
    }
}
