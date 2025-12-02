package com.example;

/**
 * 테스트용 예제 클래스
 * 다양한 if문 분기를 포함
 */
public class Calculator {

    /**
     * 숫자를 평가하는 메서드
     * 여러 조건을 포함
     */
    public String evaluateNumber(int number, boolean isPositive) {
        if (number > 0 && isPositive) {
            return "Positive and valid";
        }

        if (number < 0) {
            return "Negative";
        }

        if (number == 0) {
            return "Zero";
        }

        return "Unknown";
    }

    /**
     * 사용자 권한을 확인하는 메서드
     */
    public boolean checkPermission(int age, String role) {
        if (age >= 18 && role.equals("admin")) {
            return true;
        }

        if (age >= 13 && role.equals("user")) {
            return true;
        }

        return false;
    }

    /**
     * 할인율을 계산하는 메서드
     */
    public double calculateDiscount(int quantity, double price, boolean isMember) {
        double discount = 0.0;

        if (quantity > 100) {
            discount = 0.2;
        } else if (quantity > 50) {
            discount = 0.1;
        } else if (quantity > 10) {
            discount = 0.05;
        }

        if (isMember) {
            discount += 0.05;
        }

        if (price > 1000) {
            discount += 0.03;
        }

        return discount;
    }

    /**
     * 복잡한 조건을 가진 메서드
     */
    public String processOrder(int itemCount, double totalPrice, boolean isPremiumCustomer, String location) {
        if (itemCount > 0 && totalPrice > 0) {
            if (isPremiumCustomer && totalPrice > 500) {
                if (location.equals("domestic")) {
                    return "Free shipping with premium";
                } else {
                    return "Discounted international shipping";
                }
            }

            if (totalPrice > 100 && location.equals("domestic")) {
                return "Standard shipping";
            }

            return "Regular processing";
        }

        return "Invalid order";
    }
}
