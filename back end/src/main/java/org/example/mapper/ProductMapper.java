package org.example.mapper;

import org.example.dto.CategoryDTO;
import org.example.dto.ProductDTO;
import org.example.model.Category;
import org.example.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryName", source = "category.name")
    ProductDTO toDTO(Product product);

    CategoryDTO toDTO(Category category);
}