package com.payflow.gateway.analytics.dto;

public record DailyVolume(
    String date,
    Long volumeMinor,
    Long count
) {}

