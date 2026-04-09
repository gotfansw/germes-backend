package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCategoryRequest {

    @NotBlank(message = "Название категории не может быть пустым")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}