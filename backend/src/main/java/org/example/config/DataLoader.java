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
        Category литые    = findOrCreateCategory("Литые диски (набивные)");
        Category штамп    = findOrCreateCategory("Штампованные диски (набивные)");
        Category selfPb   = findOrCreateCategory("Самоклеящиеся (Pb)");
        Category selfZn   = findOrCreateCategory("Самоклеящиеся (Zn)");

        // Литые диски
        findOrCreateProduct("Груза набивные, для литых дисков 5гр. (100шт.)",   "471.45",  литые);
        findOrCreateProduct("Груза набивные, для литых дисков 10гр. (100шт.)",  "640.51",  литые);
        findOrCreateProduct("Груза набивные, для литых дисков 15гр. (100шт.)",  "862.8",   литые);
        findOrCreateProduct("Груза набивные, для литых дисков 20гр. (100шт.)",  "1087.81", литые);
        findOrCreateProduct("Груза набивные, для литых дисков 25гр. (100шт.)",  "1313.57", литые);
        findOrCreateProduct("Груза набивные, для литых дисков 30гр. (100шт.)",  "1536.59", литые);
        findOrCreateProduct("Груза набивные, для литых дисков 35гр. (50шт.)",   "900.6",   литые);
        findOrCreateProduct("Груза набивные, для литых дисков 40гр. (50шт.)",   "1011.16", литые);
        findOrCreateProduct("Груза набивные, для литых дисков 45гр. (50шт.)",   "1127.4",  литые);
        findOrCreateProduct("Груза набивные, для литых дисков 50гр. (50шт.)",   "1240.81", литые);
        findOrCreateProduct("Груза набивные, для литых дисков 55гр. (50шт.)",   "1350.32", литые);
        findOrCreateProduct("Груза набивные, для литых дисков 60гр. (50шт.)",   "1466.86", литые);

        // Штампованные диски
        findOrCreateProduct("Грузики для шиномонтажа на штамп. диски 200шт. по 5гр.",             "785.86",  штамп);
        findOrCreateProduct("Грузики для шиномонтажа, для штамповочных дисков, 10гр. (100шт.)",   "602.92",  штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 15гр., (100шт.)",            "825",     штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 20гр., (100шт.)",            "1048.96", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 25гр., (100шт.)",            "1272.94", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 30гр., (100шт.)",            "1351.36", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 35гр., (50шт.)",             "863.74",  штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 40гр., (50шт.)",             "993.31",  штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 45гр., (50шт.)",             "1106.14", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 50гр., (50шт.)",             "1220.11", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 55гр., (50шт.)",             "1403.35", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 60гр., (50шт.)",             "1517.68", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 70гр., (25шт.)",             "888.31",  штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 90гр., (25шт.)",             "1120.78", штамп);
        findOrCreateProduct("Груза набивные, для штампованных дисков, 100гр., (25шт.)",            "1234.19", штамп);

        // Самоклеящиеся Pb
        findOrCreateProduct("Самоклеящиеся свинцовые груза Спорт N 60г (50шт)",                   "1263",    selfPb);
        findOrCreateProduct("Самоклеящиеся свинцовые груза Стандарт N 60г (50шт)",                "1246.37", selfPb);
        findOrCreateProduct("Грузики для шиномонтажа, самоклеящиеся, Pb, 200гр (15 шт)",          "1551.71", selfPb);

        // Самоклеящиеся Zn
        findOrCreateProduct("Грузики для шиномонтажа самоклеящиеся, Zn 60гр. 50шт",              "689.85",  selfZn);
    }

    private Category findOrCreateCategory(String name) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(name);
                    return categoryRepository.save(c);
                });
    }

    private void findOrCreateProduct(String name, String price, Category category) {
        if (productRepository.existsByName(name)) return;
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setCategory(category);
        productRepository.save(p);
    }
}