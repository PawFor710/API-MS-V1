package com.pawfor.vertx.microservice.domain;

import java.util.UUID;

public class Item {

    private UUID id;
    private String ownerId;
    private String name;

    public Item(UUID id, String ownerId, String name) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
    }

    public Item() {
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setName(String name) {
        this.name = name;
    }
}
