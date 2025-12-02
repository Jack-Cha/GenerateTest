package com.example;

/**
 * 상품 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
public class ProductService {

    /**
     * 상품 유효성 검증
     */
    public boolean isValidProduct(ProductDTO product) {
        if (product == null) {
            return false;
        }

        // 상품 ID 검증
        if (product.getProductId() == null || product.getProductId().isEmpty()) {
            return false;
        }

        // 상품명 검증
        if (product.getName() == null || product.getName().isEmpty()) {
            return false;
        }

        // 상품명 길이 검증 (2~100자)
        if (product.getName().length() < 2 || product.getName().length() > 100) {
            return false;
        }

        // 가격 검증 (0보다 커야 함)
        if (product.getPrice() <= 0) {
            return false;
        }

        // 가격 상한선 검증 (1억원 이하)
        if (product.getPrice() > 100000000) {
            return false;
        }

        return true;
    }

    /**
     * 상품 가격 등급 분류
     */
    public String getPriceGrade(ProductDTO product) {
        if (product == null) {
            return "Unknown";
        }

        int price = product.getPrice();

        if (price >= 1000000) {
            return "Premium";      // 100만원 이상
        }

        if (price >= 500000) {
            return "High";         // 50만원 이상
        }

        if (price >= 100000) {
            return "Medium";       // 10만원 이상
        }

        if (price >= 10000) {
            return "Low";          // 1만원 이상
        }

        return "Budget";           // 1만원 미만
    }

    /**
     * 상품 할인가 계산
     */
    public int calculateDiscountedPrice(ProductDTO product, double discountRate) {
        if (product == null || discountRate < 0) {
            return 0;
        }

        // 할인율이 너무 크면 제한 (최대 50%)
        if (discountRate > 0.5) {
            discountRate = 0.5;
        }

        int originalPrice = product.getPrice();
        int discount = (int) (originalPrice * discountRate);

        return originalPrice - discount;
    }

    /**
     * 대량 구매 할인율 계산
     */
    public double getBulkDiscountRate(ProductDTO product, int quantity) {
        if (product == null || quantity <= 0) {
            return 0.0;
        }

        // 수량에 따른 할인
        if (quantity >= 100) {
            return 0.3;  // 30% 할인
        }

        if (quantity >= 50) {
            return 0.2;  // 20% 할인
        }

        if (quantity >= 10) {
            return 0.1;  // 10% 할인
        }

        if (quantity >= 5) {
            return 0.05; // 5% 할인
        }

        return 0.0;      // 할인 없음
    }

    /**
     * 특정 가격대 상품인지 확인
     */
    public boolean isPremiumProduct(ProductDTO product) {
        if (product == null) {
            return false;
        }

        return product.getPrice() >= 1000000;
    }

    /**
     * 배송비 계산
     */
    public int calculateShippingFee(ProductDTO product) {
        if (product == null) {
            return 0;
        }

        int price = product.getPrice();

        // 무료 배송 조건: 5만원 이상
        if (price >= 50000) {
            return 0;
        }

        // 3만원 이상: 배송비 2,500원
        if (price >= 30000) {
            return 2500;
        }

        // 1만원 이상: 배송비 3,000원
        if (price >= 10000) {
            return 3000;
        }

        // 1만원 미만: 배송비 5,000원
        return 5000;
    }

    /**
     * 세일 가능 여부 확인
     */
    public boolean canApplySale(ProductDTO product, int stockQuantity) {
        if (product == null) {
            return false;
        }

        // 재고가 없으면 세일 불가
        if (stockQuantity <= 0) {
            return false;
        }

        // 고가 제품 (100만원 이상)은 세일 불가
        if (product.getPrice() >= 1000000) {
            return false;
        }

        // 재고가 많으면 세일 가능
        if (stockQuantity >= 100) {
            return true;
        }

        // 중저가 제품 (10만원 미만)은 재고 50개 이상이면 세일 가능
        if (product.getPrice() < 100000 && stockQuantity >= 50) {
            return true;
        }

        return false;
    }

    /**
     * 최종 가격 계산 (할인 + 배송비 포함)
     */
    public int calculateFinalPrice(ProductDTO product, int quantity, double discountRate) {
        if (product == null || quantity <= 0) {
            return 0;
        }

        // 상품 가격 * 수량
        int totalPrice = product.getPrice() * quantity;

        // 할인 적용
        if (discountRate > 0) {
            if (discountRate > 0.5) {
                discountRate = 0.5;  // 최대 50% 할인
            }
            totalPrice = (int) (totalPrice * (1 - discountRate));
        }

        // 배송비 추가 (총액 5만원 미만인 경우만)
        if (totalPrice < 50000) {
            int shippingFee = calculateShippingFee(product);
            totalPrice += shippingFee;
        }

        return totalPrice;
    }
}
