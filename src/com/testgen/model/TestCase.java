package com.testgen.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 생성될 테스트 케이스 정보를 저장하는 클래스
 */
public class TestCase {
    private Map<String, Object> variableValues;
    private String description;
    private int caseNumber;

    public TestCase(int caseNumber) {
        this.caseNumber = caseNumber;
        this.variableValues = new HashMap<>();
        this.description = "";
    }

    public void addVariableValue(String variableName, Object value) {
        this.variableValues.put(variableName, value);
    }

    public Map<String, Object> getVariableValues() {
        return variableValues;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCaseNumber() {
        return caseNumber;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "caseNumber=" + caseNumber +
                ", variableValues=" + variableValues +
                ", description='" + description + '\'' +
                '}';
    }
}
