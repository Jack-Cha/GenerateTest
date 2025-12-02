package com.example;

import java.util.List;

public class OrderDTO {
    private String orderId;
    private List<ProductDTO> products;  // 다른 DTO(ProductDTO)와 연결

    public OrderDTO(String orderId, List<ProductDTO> products) {
        this.orderId = orderId;
        this.products = products;
    }

    // getter & setter
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public List<ProductDTO> getProducts() { return products; }
    public void setProducts(List<ProductDTO> products) { this.products = products; }
}