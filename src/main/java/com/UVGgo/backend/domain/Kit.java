package com.UVGgo.backend.domain;

import java.math.BigDecimal;

public class Kit {
    private int kitId;
    private String name;
    private String description;
    private String course;
    private BigDecimal price;
    private boolean active;

    public int getKitId() {
        return kitId;
    }

    public void setKitId(int kitId) {
        this.kitId = kitId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
