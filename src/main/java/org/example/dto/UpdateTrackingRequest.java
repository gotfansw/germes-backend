package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateTrackingRequest {

    @NotBlank(message = "Трек-номер обязателен")
    private String trackNumber;

    public String getTrackNumber() { return trackNumber; }
    public void setTrackNumber(String trackNumber) { this.trackNumber = trackNumber; }
}
