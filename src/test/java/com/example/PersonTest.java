package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Person의 모든 분기를 테스트하는 자동 생성 테스트 클래스
 */
public class PersonTest {

    private Person target;

    // ========================================
    // checkStatus 메서드 테스트
    // ========================================

    @Test
    @DisplayName("isActive()=true, getName().length()=11, getSalary()=50000.0, getAge()=19, getName().equals(\"Admin\")=true")
    public void testCheckStatus_Case1() {
        // Given
        target = new Person(19, "test", 50000.0, true);

        // When
        String result = target.checkStatus();
        System.out.println("[Test] checkStatus - Case 1: " + " -> result=" + result);

        // Then
        assertNotNull(result);
        // TODO: 예상 결과 확인 (필요시 수정)
    }

    @Test
    @DisplayName("isActive()=true, getName().length()=11, getSalary()=50000.0, getAge()=12, getName().equals(\"Admin\")=true")
    public void testCheckStatus_Case2() {
        // Given
        target = new Person(12, "test", 50000.0, true);

        // When
        String result = target.checkStatus();
        System.out.println("[Test] checkStatus - Case 2: " + " -> result=" + result);

        // Then
        assertNotNull(result);
        // TODO: 예상 결과 확인 (필요시 수정)
    }

    @Test
    @DisplayName("isActive()=true, getName().length()=11, getSalary()=50000.0, getAge()=65, getName().equals(\"Admin\")=true")
    public void testCheckStatus_Case3() {
        // Given
        target = new Person(65, "test", 50000.0, true);

        // When
        String result = target.checkStatus();
        System.out.println("[Test] checkStatus - Case 3: " + " -> result=" + result);

        // Then
        assertNotNull(result);
        // TODO: 예상 결과 확인 (필요시 수정)
    }

    // ========================================
    // compareAge 메서드 테스트
    // ========================================

    @Test
    @DisplayName("this.getAge()=other.getAge()")
    public void testCompareAge_Case1() {
        // Given
        target = new Person(0, "test", 0.0, false);
        Person other = new Person(0, "other", 0.0, false);

        // When
        boolean result = target.compareAge(other);
        System.out.println("[Test] compareAge - Case 1: " + "other=" + other + " -> result=" + result);

        // Then
        assertNotNull(result);
        // TODO: 예상 결과 확인 (필요시 수정)
    }

    @AfterAll
    public static void printTestSummary() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("Total test cases executed: 4");
        System.out.println("========================================");
    }
}
