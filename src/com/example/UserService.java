package com.example;

import java.util.List;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
public class UserService {

    /**
     * 사용자 등급 계산
     */
    public String calculateUserGrade(UserDTO user) {
        if (user == null) {
            return "Unknown";
        }

        List<OrderDTO> orders = user.getOrders();

        if (orders == null || orders.isEmpty()) {
            return "Bronze";
        }

        int orderCount = orders.size();

        // VIP: 주문 10개 이상
        if (orderCount >= 10) {
            return "VIP";
        }

        // Gold: 주문 5개 이상
        if (orderCount >= 5) {
            return "Gold";
        }

        // Silver: 주문 1개 이상
        if (orderCount >= 1) {
            return "Silver";
        }

        return "Bronze";
    }

    /**
     * 사용자 총 구매 금액 계산
     */
    public int calculateTotalPurchase(UserDTO user) {
        if (user == null || user.getOrders() == null) {
            return 0;
        }

        int total = 0;

        for (OrderDTO order : user.getOrders()) {
            if (order == null || order.getProducts() == null) {
                continue;
            }

            for (ProductDTO product : order.getProducts()) {
                if (product != null) {
                    total += product.getPrice();
                }
            }
        }

        return total;
    }

    /**
     * 사용자 유효성 검증
     */
    public boolean isValidUser(UserDTO user) {
        if (user == null) {
            return false;
        }

        // userId 검증
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            return false;
        }

        // userId 길이 검증 (3~20자)
        if (user.getUserId().length() < 3 || user.getUserId().length() > 20) {
            return false;
        }

        // 이름 검증
        if (user.getName() == null || user.getName().isEmpty()) {
            return false;
        }

        // 이름 길이 검증 (2~50자)
        if (user.getName().length() < 2 || user.getName().length() > 50) {
            return false;
        }

        return true;
    }

    /**
     * 사용자 활동 상태 확인
     */
    public String getUserActivityStatus(UserDTO user) {
        if (user == null || user.getOrders() == null) {
            return "Inactive";
        }

        int orderCount = user.getOrders().size();

        if (orderCount == 0) {
            return "Inactive";
        }

        if (orderCount >= 20) {
            return "Very Active";
        }

        if (orderCount >= 10) {
            return "Active";
        }

        if (orderCount >= 3) {
            return "Moderate";
        }

        return "Low Activity";
    }

    /**
     * VIP 등급 여부 확인
     */
    public boolean isVIPUser(UserDTO user) {
        if (user == null || user.getOrders() == null) {
            return false;
        }

        int orderCount = user.getOrders().size();
        int totalPurchase = calculateTotalPurchase(user);

        // 조건 1: 주문 10개 이상
        if (orderCount >= 10) {
            return true;
        }

        // 조건 2: 총 구매 금액 5백만원 이상
        if (totalPurchase >= 5000000) {
            return true;
        }

        // 조건 3: 주문 5개 이상 + 총 구매 3백만원 이상
        if (orderCount >= 5 && totalPurchase >= 3000000) {
            return true;
        }

        return false;
    }

    /**
     * 사용자에게 적용 가능한 할인율 계산
     */
    public double getAvailableDiscount(UserDTO user) {
        if (user == null) {
            return 0.0;
        }

        String grade = calculateUserGrade(user);

        if (grade.equals("VIP")) {
            return 0.2;  // 20% 할인
        }

        if (grade.equals("Gold")) {
            return 0.15;  // 15% 할인
        }

        if (grade.equals("Silver")) {
            return 0.1;  // 10% 할인
        }

        if (grade.equals("Bronze")) {
            return 0.05;  // 5% 할인
        }

        return 0.0;
    }
}
