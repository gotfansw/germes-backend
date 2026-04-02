package org.example.repository;

import org.example.model.Category;
import org.example.model.Product;
import org.hibernate.Session;

public class ProductRepository {

    private Session session;

    public ProductRepository(Session session) {
        this.session = session;
    }

    public void saveCategory(Category cat) {
        session.persist(cat);
    }

    public void saveProduct(Product p) {
        session.persist(p);
    }
}