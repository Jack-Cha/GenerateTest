package com.testgen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * if문의 조건 정보를 저장하는 클래스
 */
public class ConditionInfo {
    private String expression;
    private List<VariableCondition> variableConditions;
    private int lineNumber;
    private int chainIndex;          // if-else-if 체인 내 위치 (0=if, 1=첫번째 else if, ...)
    private boolean isMutuallyExclusive;  // 상호 배타적 여부
    private String chainId;          // 같은 체인에 속한 조건들의 그룹 ID

    public ConditionInfo(String expression, int lineNumber) {
        this.expression = expression;
        this.lineNumber = lineNumber;
        this.variableConditions = new ArrayList<>();
        this.chainIndex = -1;  // 독립적인 if문
        this.isMutuallyExclusive = false;
        this.chainId = null;
    }

    public ConditionInfo(String expression, int lineNumber, int chainIndex, boolean isMutuallyExclusive, String chainId) {
        this.expression = expression;
        this.lineNumber = lineNumber;
        this.variableConditions = new ArrayList<>();
        this.chainIndex = chainIndex;
        this.isMutuallyExclusive = isMutuallyExclusive;
        this.chainId = chainId;
    }

    public void addVariableCondition(VariableCondition condition) {
        this.variableConditions.add(condition);
    }

    public String getExpression() {
        return expression;
    }

    public List<VariableCondition> getVariableConditions() {
        return variableConditions;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getChainIndex() {
        return chainIndex;
    }

    public boolean isMutuallyExclusive() {
        return isMutuallyExclusive;
    }

    public String getChainId() {
        return chainId;
    }

    @Override
    public String toString() {
        return "ConditionInfo{" +
                "expression='" + expression + '\'' +
                ", variableConditions=" + variableConditions +
                ", lineNumber=" + lineNumber +
                '}';
    }

    /**
     * 변수와 조건을 나타내는 내부 클래스
     */
    public static class VariableCondition {
        private String variableName;
        private String operator;
        private String value;
        private String type;

        public VariableCondition(String variableName, String operator, String value, String type) {
            this.variableName = variableName;
            this.operator = operator;
            this.value = value;
            this.type = type;
        }

        public String getVariableName() {
            return variableName;
        }

        public String getOperator() {
            return operator;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VariableCondition that = (VariableCondition) o;
            return Objects.equals(variableName, that.variableName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(variableName);
        }

        @Override
        public String toString() {
            return "VariableCondition{" +
                    "variableName='" + variableName + '\'' +
                    ", operator='" + operator + '\'' +
                    ", value='" + value + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
