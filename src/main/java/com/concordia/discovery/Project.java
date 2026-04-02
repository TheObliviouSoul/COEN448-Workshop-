package com.concordia.discovery;

public record Project(
        String id,
        String title,
        String location,
        String scheduledTime,
        String abstractText) {
}
