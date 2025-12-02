package com.example;

public class Person {
    private int age;
    private String name;
    private double salary;
    private boolean active;

    public Person(int age, String name, double salary, boolean active) {
        this.age = age;
        this.name = name;
        this.salary = salary;
        this.active = active;
    }

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }

    public double getSalary() {
        return salary;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * getter를 사용한 조건문 테스트
     */
    public String checkStatus() {
        // getter와 비교 연산자
        if (getAge() > 18) {
            return "Adult";
        }

        if (getAge() < 13) {
            return "Child";
        }

        // boolean getter
        if (isActive()) {
            return "Active User";
        }

        // 복합 조건 with getter
        if (getAge() >= 65 && getSalary() > 50000.0) {
            return "Senior with good salary";
        }

        // String 메서드 호출
        if (getName().equals("Admin")) {
            return "Administrator";
        }

        // length 메서드
        if (getName().length() > 10) {
            return "Long name";
        }

        return "Unknown";
    }

    /**
     * 두 getter를 비교하는 경우
     */
    public boolean compareAge(Person other) {
        if (this.getAge() > other.getAge()) {
            return true;
        }
        return false;
    }
}
