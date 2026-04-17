package org.example.config;

import org.example.model.Category;
import org.example.model.Product;
import org.example.repository.CategoryRepository;
import org.example.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public DataLoader(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {

        if (!categoryRepository.existsById(1L)) {
            Category category = new Category();
            category.setId(1L);
            category.setName("Литые диски (набивные)");
            categoryRepository.save(category);
        }
        if (!categoryRepository.existsById(2L)) {
            Category category = new Category();
            category.setId(2L);
            category.setName("Штампованные диски (набивные)");
            categoryRepository.save(category);
        }
        if (!categoryRepository.existsById(3L)) {
            Category category = new Category();
            category.setId(3L);
            category.setName("Самоклеящиеся (Pb)");
            categoryRepository.save(category);
        }
        if (!categoryRepository.existsById(4L)) {
            Category category = new Category();
            category.setId(4L);
            category.setName("Самоклеящиеся (Zn)");
            categoryRepository.save(category);
        }


        if (!productRepository.existsById(1L)) {
            Product product = new Product();
            product.setId(1L);
            product.setName("Груза набивные, для литых дисков 5гр. (100шт.)");
            product.setPrice(new BigDecimal("471.45"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(2L)) {
            Product product = new Product();
            product.setId(2L);
            product.setName("Груза набивные, для литых дисков 10гр. (100шт.)");
            product.setPrice(new BigDecimal("640.51"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(3L)) {
            Product product = new Product();
            product.setId(3L);
            product.setName("Груза набивные, для литых дисков 15гр. (100шт.)");
            product.setPrice(new BigDecimal("862.8"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(4L)) {
            Product product = new Product();
            product.setId(4L);
            product.setName("Груза набивные, для литых дисков 20гр. (100шт.)");
            product.setPrice(new BigDecimal("1087.81"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(5L)) {
            Product product = new Product();
            product.setId(5L);
            product.setName("Груза набивные, для литых дисков 25гр. (100шт.)");
            product.setPrice(new BigDecimal("1313.57"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(6L)) {
            Product product = new Product();
            product.setId(6L);
            product.setName("Груза набивные, для литых дисков 30гр. (100шт.)");
            product.setPrice(new BigDecimal("1536.59"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(7L)) {
            Product product = new Product();
            product.setId(7L);
            product.setName("Груза набивные, для литых дисков 35гр. (50шт.)");
            product.setPrice(new BigDecimal("900.6"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(8L)) {
            Product product = new Product();
            product.setId(8L);
            product.setName("Груза набивные, для литых дисков 40гр. (50шт.)");
            product.setPrice(new BigDecimal("1011.16"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(9L)) {
            Product product = new Product();
            product.setId(9L);
            product.setName("Груза набивные, для литых дисков 45гр. (50шт.)");
            product.setPrice(new BigDecimal("1127.4"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(10L)) {
            Product product = new Product();
            product.setId(10L);
            product.setName("Груза набивные, для литых дисков 50гр. (50шт.)");
            product.setPrice(new BigDecimal("1240.81"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(11L)) {
            Product product = new Product();
            product.setId(11L);
            product.setName("Груза набивные, для литых дисков 55гр. (50шт.)");
            product.setPrice(new BigDecimal("1350.32"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(12L)) {
            Product product = new Product();
            product.setId(12L);
            product.setName("Груза набивные, для литых дисков 60гр. (50шт.)");
            product.setPrice(new BigDecimal("1466.86"));
            product.setCategory(categoryRepository.findById(1L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(13L)) {
            Product product = new Product();
            product.setId(13L);
            product.setName("Грузики для шиномонтажа на штамп. диски 200шт. по 5гр.");
            product.setPrice(new BigDecimal("785.86"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(14L)) {
            Product product = new Product();
            product.setId(14L);
            product.setName("Грузики для шиномонтажа, для штамповочных дисков, 10гр. (100шт.)");
            product.setPrice(new BigDecimal("602.92"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(15L)) {
            Product product = new Product();
            product.setId(15L);
            product.setName("Груза набивные, для штампованных дисков, 15гр., (100шт.)");
            product.setPrice(new BigDecimal("825"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(16L)) {
            Product product = new Product();
            product.setId(16L);
            product.setName("Груза набивные, для штампованных дисков, 20гр., (100шт.)");
            product.setPrice(new BigDecimal("1048.96"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(17L)) {
            Product product = new Product();
            product.setId(17L);
            product.setName("Груза набивные, для штампованных дисков, 25гр., (100шт.)");
            product.setPrice(new BigDecimal("1272.94"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(18L)) {
            Product product = new Product();
            product.setId(18L);
            product.setName("Груза набивные, для штампованных дисков, 30гр., (100шт.)");
            product.setPrice(new BigDecimal("1351.36"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(19L)) {
            Product product = new Product();
            product.setId(19L);
            product.setName("Груза набивные, для штампованных дисков, 35гр., (50шт.)");
            product.setPrice(new BigDecimal("863.74"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(20L)) {
            Product product = new Product();
            product.setId(20L);
            product.setName("Груза набивные, для штампованных дисков, 40гр., (50шт.)");
            product.setPrice(new BigDecimal("993.31"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(21L)) {
            Product product = new Product();
            product.setId(21L);
            product.setName("Груза набивные, для штампованных дисков, 45гр., (50шт.)");
            product.setPrice(new BigDecimal("1106.14"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(22L)) {
            Product product = new Product();
            product.setId(22L);
            product.setName("Груза набивные, для штампованных дисков, 50гр., (50шт.)");
            product.setPrice(new BigDecimal("1220.11"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(23L)) {
            Product product = new Product();
            product.setId(23L);
            product.setName("Груза набивные, для штампованных дисков, 55гр., (50шт.)");
            product.setPrice(new BigDecimal("1403.35"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(24L)) {
            Product product = new Product();
            product.setId(24L);
            product.setName("Груза набивные, для штампованных дисков, 60гр., (50шт.)");
            product.setPrice(new BigDecimal("1517.68"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(25L)) {
            Product product = new Product();
            product.setId(25L);
            product.setName("Груза набивные, для штампованных дисков, 70гр., (25шт.)");
            product.setPrice(new BigDecimal("888.31"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(26L)) {
            Product product = new Product();
            product.setId(26L);
            product.setName("Груза набивные, для штампованных дисков, 90гр., (25шт.)");
            product.setPrice(new BigDecimal("1120.78"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(27L)) {
            Product product = new Product();
            product.setId(27L);
            product.setName("Груза набивные, для штампованных дисков, 100гр., (25шт.)");
            product.setPrice(new BigDecimal("1234.19"));
            product.setCategory(categoryRepository.findById(2L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(28L)) {
            Product product = new Product();
            product.setId(28L);
            product.setName("Самоклеящиеся свинцовые груза Спорт N 60г (50шт)");
            product.setPrice(new BigDecimal("1263"));
            product.setCategory(categoryRepository.findById(3L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(29L)) {
            Product product = new Product();
            product.setId(29L);
            product.setName("Самоклеящиеся свинцовые груза Стандарт N 60г (50шт)");
            product.setPrice(new BigDecimal("1246.37"));
            product.setCategory(categoryRepository.findById(3L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(30L)) {
            Product product = new Product();
            product.setId(30L);
            product.setName("Грузики для шиномонтажа, самоклеящиеся, Pb, 200гр (15 шт)");
            product.setPrice(new BigDecimal("1551.71"));
            product.setCategory(categoryRepository.findById(3L).orElseThrow());
            productRepository.save(product);
        }
        if (!productRepository.existsById(31L)) {
            Product product = new Product();
            product.setId(31L);
            product.setName("Грузики для шиномонтажа самоклеящиеся, Zn 60гр. 50шт");
            product.setPrice(new BigDecimal("689.85"));
            product.setCategory(categoryRepository.findById(4L).orElseThrow());
            productRepository.save(product);
        }
    }
}
