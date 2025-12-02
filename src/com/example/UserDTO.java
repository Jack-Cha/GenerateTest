package com.example;

import java.util.List;

public class UserDTO {
    private String userId;
    private String name;
    private List<OrderDTO> orders;  // 또 다른 DTO(OrderDTO)와 연결

    public UserDTO(String userId, String name, List<OrderDTO> orders) {
        this.userId = userId;
        this.name = name;
        this.orders = orders;
    }

    // getter & setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<OrderDTO> getOrders() { return orders; }
    public void setOrders(List<OrderDTO> orders) { this.orders = orders; }
}