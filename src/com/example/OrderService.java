package com.example;

import java.util.Arrays;
import java.util.List;

/**
 * 주문 서비스 - 비즈니스 로직 포함
 */
public class OrderService {

    /**
     * 주문 총액 계산
     */
    public int calculateTotalPrice(OrderDTO order) {
        if (order == null || order.getProducts() == null) {
            return 0;
        }

        int total = 0;
        List<ProductDTO> products = order.getProducts();

        if (products.isEmpty()) {
            return 0;
        }

        for (ProductDTO product : products) {
            if (product != null) {
                total += product.getPrice();
            }
        }

        return total;
    }

    /**
     * 할인율 계산
     */
    public double calculateDiscount(UserDTO user, int totalPrice) {
        if (user == null) {
            return 0.0;
        }

        // 주문 개수에 따른 할인
        if (user.getOrders() != null && user.getOrders().size() > 5) {
            return 0.2;  // 20% 할인
        }

        // 총액에 따른 할인
        if (totalPrice > 1000000) {
            return 0.15;  // 15% 할인
        }

        if (totalPrice > 500000) {
            return 0.1;  // 10% 할인
        }

        return 0.0;  // 할인 없음
    }

    /**
     * 사용자 등급 확인
     */
    public String getUserGrade(UserDTO user) {
        if (user == null || user.getOrders() == null) {
            return "Guest";
        }

        int orderCount = user.getOrders().size();

        if (orderCount >= 10) {
            return "VIP";
        }

        if (orderCount >= 5) {
            return "Gold";
        }

        if (orderCount >= 1) {
            return "Silver";
        }

        return "Bronze";
    }

    /**
     * 주문 유효성 검증
     */
    public boolean isValidOrder(OrderDTO order) {
        if (order == null) {
            return false;
        }

        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            return false;
        }

        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            return false;
        }

        // 모든 상품이 유효한지 확인
        for (ProductDTO product : order.getProducts()) {
            if (product == null) {
                return false;
            }

            if (product.getPrice() <= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * 메인 메서드 - 사용 예제
     */
    public static void main(String[] args) {
        OrderService service = new OrderService();

        // ProductDTO 생성
        ProductDTO product1 = new ProductDTO("P001", "노트북", 1500000);
        ProductDTO product2 = new ProductDTO("P002", "마우스", 30000);

        // OrderDTO 생성
        OrderDTO order = new OrderDTO("O1001", Arrays.asList(product1, product2));

        // UserDTO 생성
        UserDTO user = new UserDTO("U01", "홍길동", Arrays.asList(order));

        // 비즈니스 로직 실행
        int totalPrice = service.calculateTotalPrice(order);
        double discount = service.calculateDiscount(user, totalPrice);
        String grade = service.getUserGrade(user);
        boolean isValid = service.isValidOrder(order);

        // 출력
        System.out.println("사용자: " + user.getName());
        System.out.println("주문 ID: " + order.getOrderId());
        System.out.println("첫 번째 상품: " + product1.getName());
        System.out.println("총액: " + totalPrice + "원");
        System.out.println("할인율: " + (discount * 100) + "%");
        System.out.println("사용자 등급: " + grade);
        System.out.println("유효한 주문: " + isValid);
    }
}
