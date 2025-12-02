package com.testgen.generator;

import com.testgen.model.ConditionInfo;
import com.testgen.model.ConditionInfo.VariableCondition;
import com.testgen.model.TestCase;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * JUnit 테스트 코드를 생성하는 클래스
 */
public class JUnitTestGenerator {
    private String packageName;
    private String className;
    private String testClassName;

    public JUnitTestGenerator(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;
        this.testClassName = className + "Test";
    }

    /**
     * 메서드의 모든 조건 조합에 대한 테스트 케이스 생성
     */
    public List<TestCase> generateTestCases(List<ConditionInfo> conditions) {
        List<TestCase> testCases = new ArrayList<>();

        // if-else-if 체인별로 그룹화
        Map<String, List<ConditionInfo>> chains = groupByChain(conditions);

        // 독립적인 if문들
        List<ConditionInfo> independentConditions = getIndependentConditions(conditions);

        if (chains.isEmpty()) {
            // 체인이 없으면 기존 방식대로 처리
            return generateTestCasesLegacy(conditions);
        }

        // 각 체인에서 한 브랜치씩 선택하여 테스트 생성
        List<List<ConditionInfo>> chainCombinations = generateChainCombinations(chains);

        int caseNumber = 1;
        for (List<ConditionInfo> chainCombo : chainCombinations) {
            // 선택된 브랜치들 + 독립적인 조건들
            List<ConditionInfo> allConditions = new ArrayList<>(chainCombo);
            allConditions.addAll(independentConditions);

            // 이제 조합 생성 (체인 브랜치는 대표값만 사용)
            Map<String, Set<String>> variableValues = collectVariableValuesForChain(allConditions, chainCombo);
            List<Map<String, Object>> combinations = generateCombinations(variableValues);

            for (Map<String, Object> combination : combinations) {
                TestCase testCase = new TestCase(caseNumber++);
                combination.forEach(testCase::addVariableValue);
                testCase.setDescription(generateDescription(combination, allConditions));
                testCases.add(testCase);
            }
        }

        return testCases;
    }

    /**
     * 기존 방식의 테스트 케이스 생성 (상호 배타적 조건 없을 때)
     * 옵션 2: 대표값만 사용하도록 수정
     */
    private List<TestCase> generateTestCasesLegacy(List<ConditionInfo> conditions) {
        List<TestCase> testCases = new ArrayList<>();

        // 대표값만 수집 (경계값 제거)
        Map<String, Set<String>> variableValues = collectVariableValuesSimplified(conditions);
        List<Map<String, Object>> combinations = generateCombinations(variableValues);

        int caseNumber = 1;
        for (Map<String, Object> combination : combinations) {
            TestCase testCase = new TestCase(caseNumber++);
            combination.forEach(testCase::addVariableValue);
            testCase.setDescription(generateDescription(combination, conditions));
            testCases.add(testCase);
        }

        return testCases;
    }

    /**
     * 단순화된 변수 값 수집 (대표값만, 경계값 제거)
     */
    private Map<String, Set<String>> collectVariableValuesSimplified(List<ConditionInfo> conditions) {
        Map<String, Set<String>> variableValues = new HashMap<>();

        for (ConditionInfo condition : conditions) {
            for (VariableCondition vc : condition.getVariableConditions()) {
                String varName = vc.getVariableName();

                variableValues.putIfAbsent(varName, new LinkedHashSet<>());

                // 대표값만 추가
                String representativeValue = getRepresentativeValue(vc);
                variableValues.get(varName).add(representativeValue);

                // boolean은 true/false 둘 다 필요
                if ("boolean".equals(vc.getType())) {
                    variableValues.get(varName).add("true");
                    variableValues.get(varName).add("false");
                }
            }
        }

        return variableValues;
    }

    /**
     * if-else-if 체인별로 조건들을 그룹화
     */
    private Map<String, List<ConditionInfo>> groupByChain(List<ConditionInfo> conditions) {
        Map<String, List<ConditionInfo>> chains = new HashMap<>();

        for (ConditionInfo condition : conditions) {
            if (condition.isMutuallyExclusive() && condition.getChainId() != null) {
                String chainId = condition.getChainId();
                chains.putIfAbsent(chainId, new ArrayList<>());
                chains.get(chainId).add(condition);
            }
        }

        return chains;
    }

    /**
     * 독립적인 if문들 추출
     */
    private List<ConditionInfo> getIndependentConditions(List<ConditionInfo> conditions) {
        List<ConditionInfo> independent = new ArrayList<>();

        for (ConditionInfo condition : conditions) {
            if (!condition.isMutuallyExclusive() || condition.getChainId() == null) {
                independent.add(condition);
            }
        }

        return independent;
    }

    /**
     * 각 체인에서 한 브랜치씩 선택하는 모든 조합 생성
     */
    private List<List<ConditionInfo>> generateChainCombinations(
            Map<String, List<ConditionInfo>> chains) {

        if (chains.isEmpty()) {
            return Arrays.asList(new ArrayList<>());
        }

        List<List<ConditionInfo>> result = new ArrayList<>();
        List<List<ConditionInfo>> chainLists = new ArrayList<>(chains.values());

        generateChainCombinationsHelper(chainLists, 0, new ArrayList<>(), result);

        return result;
    }

    /**
     * 체인 조합 생성 헬퍼 메서드
     */
    private void generateChainCombinationsHelper(
            List<List<ConditionInfo>> chains,
            int index,
            List<ConditionInfo> current,
            List<List<ConditionInfo>> result) {

        if (index == chains.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        // 현재 체인의 각 브랜치 시도
        for (ConditionInfo branch : chains.get(index)) {
            current.add(branch);
            generateChainCombinationsHelper(chains, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    /**
     * 각 변수에 대한 가능한 값들을 수집
     */
    private Map<String, Set<String>> collectVariableValues(List<ConditionInfo> conditions) {
        Map<String, Set<String>> variableValues = new HashMap<>();

        for (ConditionInfo condition : conditions) {
            for (VariableCondition vc : condition.getVariableConditions()) {
                String varName = vc.getVariableName();
                String value = vc.getValue();

                variableValues.putIfAbsent(varName, new LinkedHashSet<>());

                // 조건에서 사용된 값 추가
                variableValues.get(varName).add(value);

                // 경계값 및 반대 케이스 추가
                addBoundaryValues(variableValues.get(varName), vc);
            }
        }

        return variableValues;
    }

    /**
     * 체인 브랜치에 대해 대표값만 수집 (경계값 분석 최소화)
     */
    private Map<String, Set<String>> collectVariableValuesForChain(
            List<ConditionInfo> allConditions,
            List<ConditionInfo> chainBranches) {

        Map<String, Set<String>> variableValues = new HashMap<>();

        for (ConditionInfo condition : allConditions) {
            boolean isChainBranch = chainBranches.contains(condition);

            for (VariableCondition vc : condition.getVariableConditions()) {
                String varName = vc.getVariableName();
                String value = vc.getValue();

                variableValues.putIfAbsent(varName, new LinkedHashSet<>());

                if (isChainBranch) {
                    // 체인 브랜치: 대표값 1개만 추가
                    String representativeValue = getRepresentativeValue(vc);
                    variableValues.get(varName).add(representativeValue);
                } else {
                    // 독립적인 조건: 대표값만 추가 (경계값 제거)
                    String representativeValue = getRepresentativeValue(vc);
                    variableValues.get(varName).add(representativeValue);

                    // boolean 타입은 true/false 둘 다 필요
                    if ("boolean".equals(vc.getType())) {
                        variableValues.get(varName).add("true");
                        variableValues.get(varName).add("false");
                    }
                }
            }
        }

        return variableValues;
    }

    /**
     * 조건에 대한 대표값 생성
     */
    private String getRepresentativeValue(VariableCondition vc) {
        String type = vc.getType();
        String value = vc.getValue();
        String operator = vc.getOperator();

        if ("int".equals(type) || "long".equals(type)) {
            try {
                long num = Long.parseLong(value);
                // 연산자에 따라 대표값 선택
                switch (operator) {
                    case ">":
                        return String.valueOf(num + 1);  // N+1
                    case ">=":
                        return value;  // N
                    case "<":
                        return String.valueOf(num - 1);  // N-1
                    case "<=":
                        return value;  // N
                    case "==":
                        return value;  // N
                    case "!=":
                        return String.valueOf(num + 1);  // N+1 (다른 값)
                    default:
                        return value;
                }
            } catch (NumberFormatException e) {
                return value;
            }
        } else if ("boolean".equals(type)) {
            // boolean은 이미 대표값 (true 또는 false)
            return value.equals("true") ? "true" : "false";
        } else {
            // String이나 다른 타입은 원래 값
            return value;
        }
    }

    /**
     * 경계값 및 반대 케이스 값 추가
     */
    private void addBoundaryValues(Set<String> values, VariableCondition vc) {
        String type = vc.getType();
        String value = vc.getValue();
        String operator = vc.getOperator();

        if ("int".equals(type) || "long".equals(type)) {
            try {
                long num = Long.parseLong(value);
                values.add(String.valueOf(num - 1)); // 작은 값
                values.add(String.valueOf(num + 1)); // 큰 값

                if (operator.equals("==") || operator.equals("!=")) {
                    values.add("0"); // 기본값
                }
            } catch (NumberFormatException e) {
                // 숫자가 아닌 경우 무시
            }
        } else if ("double".equals(type)) {
            try {
                double num = Double.parseDouble(value);
                values.add(String.valueOf(num - 1.0));
                values.add(String.valueOf(num + 1.0));
            } catch (NumberFormatException e) {
                // 숫자가 아닌 경우 무시
            }
        } else if ("boolean".equals(type)) {
            values.add("true");
            values.add("false");
        } else if ("String".equals(type)) {
            values.add(value); // 원래 값
            values.add("\"\""); // 빈 문자열
            values.add("null"); // null
        }
    }

    /**
     * 모든 변수 값의 조합 생성 (카르테시안 곱)
     */
    private List<Map<String, Object>> generateCombinations(Map<String, Set<String>> variableValues) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<String> variables = new ArrayList<>(variableValues.keySet());
        List<List<String>> valueLists = new ArrayList<>();

        for (String var : variables) {
            valueLists.add(new ArrayList<>(variableValues.get(var)));
        }

        generateCombinationsRecursive(variables, valueLists, 0, new HashMap<>(), result);

        return result;
    }

    /**
     * 재귀적으로 조합 생성
     */
    private void generateCombinationsRecursive(
            List<String> variables,
            List<List<String>> valueLists,
            int index,
            Map<String, Object> current,
            List<Map<String, Object>> result) {

        if (index == variables.size()) {
            result.add(new HashMap<>(current));
            return;
        }

        String variable = variables.get(index);
        List<String> values = valueLists.get(index);

        for (String value : values) {
            current.put(variable, parseValue(value));
            generateCombinationsRecursive(variables, valueLists, index + 1, current, result);
        }
    }

    /**
     * 문자열 값을 적절한 타입으로 변환
     */
    private Object parseValue(String value) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        if ("null".equals(value)) return null;

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // 문자열로 반환
            return value.replaceAll("^\"|\"$", ""); // 따옴표 제거
        }
    }

    /**
     * 테스트 케이스 설명 생성
     */
    private String generateDescription(Map<String, Object> combination, List<ConditionInfo> conditions) {
        StringBuilder desc = new StringBuilder();
        combination.forEach((var, val) -> {
            if (desc.length() > 0) desc.append(", ");
            // 변수명과 값에서 따옴표 이스케이프 처리
            String escapedVar = var.replace("\"", "\\\"");
            String escapedVal = String.valueOf(val).replace("\"", "\\\"");
            desc.append(escapedVar).append("=").append(escapedVal);
        });
        return desc.toString();
    }

    /**
     * JUnit 테스트 코드 생성 (모든 메서드에 대한 테스트를 한 파일에 생성)
     */
    public void generateTestCode(Map<String, List<TestCase>> methodTestCases,
                                 Map<String, com.github.javaparser.ast.body.MethodDeclaration> methodDeclarations,
                                 com.github.javaparser.ast.body.ConstructorDeclaration primaryConstructor,
                                 String outputPath) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 패키지 및 import 문
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import org.junit.jupiter.api.AfterAll;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");

        // 클래스 선언
        sb.append("/**\n");
        sb.append(" * ").append(className).append("의 모든 분기를 테스트하는 자동 생성 테스트 클래스\n");
        sb.append(" */\n");
        sb.append("public class ").append(testClassName).append(" {\n\n");

        // 테스트 대상 객체 생성 코드
        sb.append("    private ").append(className).append(" target");
        if (primaryConstructor != null && !primaryConstructor.getParameters().isEmpty()) {
            // 생성자에 파라미터가 있으면 나중에 각 테스트에서 생성
            sb.append(";\n\n");
        } else {
            // 기본 생성자
            sb.append(" = new ").append(className).append("();\n\n");
        }

        int totalTestCases = 0;

        // 모든 메서드에 대한 테스트 생성
        for (Map.Entry<String, List<TestCase>> entry : methodTestCases.entrySet()) {
            String methodName = entry.getKey();
            List<TestCase> testCases = entry.getValue();

            // 메서드별 주석 추가
            sb.append("    // ========================================\n");
            sb.append("    // ").append(methodName).append(" 메서드 테스트\n");
            sb.append("    // ========================================\n\n");

            // 각 테스트 케이스 생성
            com.github.javaparser.ast.body.MethodDeclaration methodDecl = methodDeclarations.get(methodName);
            for (TestCase testCase : testCases) {
                generateTestMethod(sb, methodName, testCase, methodDecl, primaryConstructor);
            }

            totalTestCases += testCases.size();
        }

        // @AfterAll 메서드 추가 - 전체 테스트 케이스 수 출력
        sb.append("    @AfterAll\n");
        sb.append("    public static void printTestSummary() {\n");
        sb.append("        System.out.println();\n");
        sb.append("        System.out.println(\"========================================\");\n");
        sb.append("        System.out.println(\"Total test cases executed: ").append(totalTestCases).append("\");\n");
        sb.append("        System.out.println(\"========================================\");\n");
        sb.append("    }\n");

        sb.append("}\n");

        // 파일 저장
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(sb.toString());
        }

        System.out.println("테스트 코드가 생성되었습니다: " + outputPath);
        System.out.println("총 " + totalTestCases + "개의 테스트 케이스가 생성되었습니다.");
    }

    /**
     * 개별 테스트 메서드 생성 (실행 가능한 코드)
     */
    private void generateTestMethod(StringBuilder sb, String methodName, TestCase testCase,
                                   com.github.javaparser.ast.body.MethodDeclaration methodDecl,
                                   com.github.javaparser.ast.body.ConstructorDeclaration primaryConstructor) {
        String testMethodName = "test" + capitalize(methodName) + "_Case" + testCase.getCaseNumber();

        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(testCase.getDescription()).append("\")\n");
        sb.append("    public void ").append(testMethodName).append("() {\n");

        // Given: 변수 선언 및 초기화
        sb.append("        // Given\n");
        Map<String, Object> varValues = testCase.getVariableValues();

        // 메서드 호출 변수를 실제 필드로 매핑
        Map<String, Object> fieldValues = extractFieldValues(varValues);

        // 생성자가 있으면 객체 생성
        if (primaryConstructor != null && !primaryConstructor.getParameters().isEmpty()) {
            sb.append("        target = new ").append(className).append("(");

            List<String> constructorArgs = new ArrayList<>();
            primaryConstructor.getParameters().forEach(param -> {
                String paramName = param.getNameAsString();
                String paramType = param.getType().asString();

                // 필드 값이 있고 타입이 맞으면 사용, 아니면 기본값
                Object value = fieldValues.get(paramName);
                if (value == null || !isCompatibleType(value, paramType)) {
                    value = getDefaultValue(paramType);
                }

                constructorArgs.add(formatValue(value));
            });

            sb.append(String.join(", ", constructorArgs));
            sb.append(");\n");
        }

        // 메서드 파라미터 정보 가져오기
        if (methodDecl != null) {
            methodDecl.getParameters().forEach(param -> {
                String paramName = param.getNameAsString();
                String paramType = param.getType().asString();

                // 파라미터 값 찾기 (other.getAge() 같은 변수에서 other 파라미터 값 추출)
                Object value = findParameterValue(paramName, paramType, varValues, fieldValues);

                sb.append("        ").append(paramType).append(" ").append(paramName)
                  .append(" = ").append(formatValue(value)).append(";\n");
            });
        }

        sb.append("\n");
        sb.append("        // When\n");

        // 실제 메서드 호출 코드 생성
        if (methodDecl != null) {
            String returnType = methodDecl.getType().asString();

            // 입력 데이터 출력 준비
            List<String> paramNames = new ArrayList<>();
            methodDecl.getParameters().forEach(param -> paramNames.add(param.getNameAsString()));

            // 메서드 호출
            sb.append("        ");
            if (!"void".equals(returnType)) {
                sb.append(returnType).append(" result = ");
            }

            sb.append("target.").append(methodName).append("(");
            sb.append(String.join(", ", paramNames));
            sb.append(");\n");

            // 한 줄로 출력: [Test] methodName - Case N: param1=value1, param2=value2 -> result=value
            sb.append("        System.out.println(\"[Test] ").append(methodName).append(" - Case ").append(testCase.getCaseNumber()).append(": \"");

            // 파라미터들을 comma로 구분
            for (int i = 0; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i);
                if (i > 0) sb.append(" + \", \"");
                sb.append(" + \"").append(paramName).append("=\" + ").append(paramName);
            }

            // 결과 출력
            if (!"void".equals(returnType)) {
                sb.append(" + \" -> result=\" + result");
            }
            sb.append(");\n\n");

            // Then: assertion 생성
            sb.append("        // Then\n");
            if (!"void".equals(returnType)) {
                sb.append("        assertNotNull(result);\n");
                sb.append("        // TODO: 예상 결과 확인 (필요시 수정)\n");
            } else {
                sb.append("        // void 메서드 - 예외가 발생하지 않으면 성공\n");
            }
        }

        sb.append("    }\n\n");
    }

    /**
     * 메서드 파라미터 값 찾기
     * other.getAge() 같은 변수에서 other 파라미터의 값들을 추출하여 객체 생성
     */
    private Object findParameterValue(String paramName, String paramType,
                                     Map<String, Object> varValues,
                                     Map<String, Object> fieldValues) {
        // 직접 파라미터 이름이 있는지 확인
        if (varValues.containsKey(paramName)) {
            return varValues.get(paramName);
        }

        // paramName.으로 시작하는 변수들 찾기 (예: other.getAge(), other.getName())
        Map<String, Object> objectFieldValues = new HashMap<>();
        String prefix = paramName + ".";

        for (Map.Entry<String, Object> entry : varValues.entrySet()) {
            String varName = entry.getKey();
            if (varName.startsWith(prefix)) {
                // other.getAge() -> getAge()
                String methodCall = varName.substring(prefix.length());

                // 메서드 체이닝 무시
                if (!isMethodChaining(methodCall) && isSimpleGetter(methodCall)) {
                    String fieldName = extractFieldNameFromMethod(methodCall);
                    objectFieldValues.put(fieldName, entry.getValue());
                }
            }
        }

        // 클래스 타입이고 필드 값이 있으면 객체 생성
        if (!objectFieldValues.isEmpty() && paramType.equals(className)) {
            // 같은 클래스 타입이면 생성자로 객체 생성
            return createObjectInstance(paramType, objectFieldValues);
        }

        // 기본값 반환
        return getDefaultValue(paramType);
    }

    /**
     * 객체 인스턴스 생성 코드 문자열 반환
     */
    private String createObjectInstance(String typeName, Map<String, Object> objectFieldValues) {
        // 생성자 호출 코드 생성
        // 필드 값을 기반으로 생성자 파라미터 결정
        List<String> args = new ArrayList<>();

        // primaryConstructor가 있으면 그 순서대로 파라미터 생성
        // 없으면 간단하게 처리
        args.add(String.valueOf(objectFieldValues.getOrDefault("age", 0)));
        args.add("\"" + objectFieldValues.getOrDefault("name", "other") + "\"");
        args.add(String.valueOf(objectFieldValues.getOrDefault("salary", 0.0)));
        args.add(String.valueOf(objectFieldValues.getOrDefault("active", false)));

        return "new " + typeName + "(" + String.join(", ", args) + ")";
    }

    /**
     * 값이 지정된 타입과 호환되는지 확인
     */
    private boolean isCompatibleType(Object value, String type) {
        if (value == null) {
            return !type.equals("int") && !type.equals("long") &&
                   !type.equals("double") && !type.equals("boolean");
        }

        switch (type) {
            case "int":
            case "long":
            case "short":
            case "byte":
                return value instanceof Integer || value instanceof Long;
            case "double":
            case "float":
                return value instanceof Double || value instanceof Float;
            case "boolean":
                return value instanceof Boolean;
            case "char":
                return value instanceof Character;
            case "String":
                return value instanceof String;
            default:
                return true;  // 객체 타입은 호환 가능
        }
    }

    /**
     * 타입에 따른 기본값 반환
     */
    private Object getDefaultValue(String type) {
        switch (type) {
            case "int":
            case "long":
            case "short":
            case "byte":
                return 0;
            case "double":
            case "float":
                return 0.0;
            case "boolean":
                return false;
            case "char":
                return 'a';
            case "String":
                return "test";
            default:
                // 클래스 타입이면 생성자 호출 코드 생성
                if (type.equals(className)) {
                    return "new " + type + "(0, \"other\", 0.0, false)";
                }
                return null;
        }
    }

    /**
     * 값을 코드 형식으로 포맷팅
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            String strValue = (String) value;
            // "new ClassName(...)" 패턴이면 그대로 반환 (생성자 호출)
            if (strValue.startsWith("new ") && strValue.contains("(")) {
                return strValue;
            }
            return "\"" + strValue + "\"";
        } else if (value instanceof Character) {
            return "'" + value + "'";
        } else {
            return value.toString();
        }
    }

    /**
     * 문자열의 첫 글자를 대문자로 변환
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 메서드 호출 변수를 실제 필드 값으로 매핑
     * 예: getAge() -> age, isActive() -> active
     * 메서드 체이닝은 무시 (getName().length() 등)
     */
    private Map<String, Object> extractFieldValues(Map<String, Object> varValues) {
        Map<String, Object> fieldValues = new HashMap<>();

        for (Map.Entry<String, Object> entry : varValues.entrySet()) {
            String varName = entry.getKey();
            Object value = entry.getValue();

            // 메서드 체이닝이 있으면 무시 (getName().equals(), getName().length() 등)
            if (isMethodChaining(varName)) {
                continue;
            }

            // 단순 getter만 처리
            if (isSimpleGetter(varName)) {
                String fieldName = extractFieldNameFromMethod(varName);
                // 이미 값이 있으면 덮어쓰지 않음 (첫 번째 값 유지)
                if (!fieldValues.containsKey(fieldName)) {
                    fieldValues.put(fieldName, value);
                }
            }
        }

        return fieldValues;
    }

    /**
     * 메서드 체이닝 여부 확인
     * 예: getName().equals("Admin"), getName().length() 등
     */
    private boolean isMethodChaining(String varName) {
        // 괄호가 있고, 그 뒤에 점(.)이 있으면 메서드 체이닝
        int parenIndex = varName.indexOf("(");
        if (parenIndex == -1) return false;

        int closeParenIndex = varName.indexOf(")", parenIndex);
        if (closeParenIndex == -1) return false;

        // 닫는 괄호 이후에 점(.)이 있으면 체이닝
        return closeParenIndex < varName.length() - 1 && varName.indexOf(".", closeParenIndex) != -1;
    }

    /**
     * 단순 getter인지 확인
     * 예: getAge(), isActive(), getName() 등
     */
    private boolean isSimpleGetter(String varName) {
        return (varName.startsWith("get") || varName.startsWith("is") || varName.startsWith("has")) &&
               varName.contains("(") && varName.endsWith(")") &&
               !isMethodChaining(varName);
    }

    /**
     * 메서드 호출에서 필드 이름 추출
     * 예: getAge() -> age, isActive() -> active, getName().equals("Admin") -> name
     */
    private String extractFieldNameFromMethod(String methodCall) {
        // 점(.) 또는 괄호 이전의 메서드 이름만 추출
        String methodName = methodCall;

        // 메서드 체이닝이 있으면 첫 번째 메서드만 추출
        if (methodCall.contains(".")) {
            int dotIndex = methodCall.indexOf(".");
            methodName = methodCall.substring(0, dotIndex);
        }

        // 괄호 제거
        if (methodName.contains("(")) {
            methodName = methodName.substring(0, methodName.indexOf("("));
        }

        // getter 패턴 처리
        if (methodName.startsWith("get") && methodName.length() > 3) {
            // getAge() -> age
            String fieldName = methodName.substring(3);
            return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            // isActive() -> active
            String fieldName = methodName.substring(2);
            return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        } else if (methodName.startsWith("has") && methodName.length() > 3) {
            // hasPermission() -> permission
            String fieldName = methodName.substring(3);
            return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        }

        // 변환할 수 없으면 원래 이름 반환
        return methodCall;
    }
}
