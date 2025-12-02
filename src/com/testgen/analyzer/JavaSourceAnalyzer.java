package com.testgen.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.IfStmt;
import com.testgen.model.ConditionInfo;
import com.testgen.model.ConditionInfo.VariableCondition;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Java 소스 코드를 파싱하여 if문의 조건들을 분석하는 클래스
 */
public class JavaSourceAnalyzer {
    private JavaParser javaParser;
    private Map<String, List<ConditionInfo>> methodConditions;
    private Map<String, MethodDeclaration> methodDeclarations;  // 메서드 정보 저장
    private String packageName;
    private String className;
    private com.github.javaparser.ast.body.ConstructorDeclaration primaryConstructor;  // 생성자 정보

    public JavaSourceAnalyzer() {
        this.javaParser = new JavaParser();
        this.methodConditions = new HashMap<>();
        this.methodDeclarations = new HashMap<>();
        this.primaryConstructor = null;
    }

    /**
     * Java 파일을 파싱하여 if문 조건들을 추출
     */
    public void analyzeFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        ParseResult<CompilationUnit> parseResult = javaParser.parse(file);

        if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
            CompilationUnit cu = parseResult.getResult().get();

            // 패키지명 추출
            this.packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            // 클래스명 추출 (첫 번째 public class)
            this.className = cu.getTypes().stream()
                    .filter(type -> type.isPublic())
                    .findFirst()
                    .map(type -> type.getNameAsString())
                    .orElse(cu.getTypes().isEmpty() ? "" : cu.getTypes().get(0).getNameAsString());

            // 생성자 찾기 (파라미터가 가장 많은 생성자 선택)
            List<com.github.javaparser.ast.body.ConstructorDeclaration> constructors =
                cu.findAll(com.github.javaparser.ast.body.ConstructorDeclaration.class);
            if (!constructors.isEmpty()) {
                this.primaryConstructor = constructors.stream()
                    .max((c1, c2) -> Integer.compare(c1.getParameters().size(), c2.getParameters().size()))
                    .orElse(constructors.get(0));
            }

            // 모든 메서드를 찾아서 분석
            cu.findAll(MethodDeclaration.class).forEach(this::analyzeMethod);
        } else {
            throw new RuntimeException("Failed to parse Java file: " + filePath);
        }
    }

    /**
     * 메서드 내의 if문들을 분석
     */
    private void analyzeMethod(MethodDeclaration method) {
        String methodName = method.getNameAsString();
        List<ConditionInfo> conditions = new ArrayList<>();
        Set<IfStmt> processedIfStmts = new HashSet<>();

        // 메서드 정보 저장
        methodDeclarations.put(methodName, method);

        // 메서드 내의 모든 if문 찾기
        method.findAll(IfStmt.class).forEach(ifStmt -> {
            // 이미 처리된 if문은 건너뛰기 (else if 체인의 일부)
            if (processedIfStmts.contains(ifStmt)) {
                return;
            }

            // if-else-if 체인 전체를 분석
            List<ConditionInfo> chain = analyzeIfElseChain(ifStmt, processedIfStmts);
            conditions.addAll(chain);
        });

        if (!conditions.isEmpty()) {
            methodConditions.put(methodName, conditions);
        }
    }

    /**
     * if-else-if 체인을 분석하여 상호 배타적인 조건들을 그룹화
     */
    private List<ConditionInfo> analyzeIfElseChain(IfStmt ifStmt, Set<IfStmt> processed) {
        List<ConditionInfo> chain = new ArrayList<>();
        IfStmt current = ifStmt;
        int chainIndex = 0;
        int firstLineNumber = ifStmt.getBegin().map(pos -> pos.line).orElse(-1);
        String chainId = "chain_" + firstLineNumber;

        // else-if가 하나라도 있으면 체인으로 인식
        boolean isChain = hasElseIfInChain(ifStmt);

        while (current != null) {
            processed.add(current);

            Expression condition = current.getCondition();
            int lineNumber = current.getBegin().map(pos -> pos.line).orElse(-1);

            // else-if 체인이면 상호 배타적으로 마킹
            ConditionInfo conditionInfo = new ConditionInfo(
                condition.toString(),
                lineNumber,
                chainIndex,
                isChain,  // else-if가 있으면 상호 배타적
                chainId
            );
            extractConditions(condition, conditionInfo);
            chain.add(conditionInfo);

            chainIndex++;

            // 다음 else 또는 else if 확인
            if (current.getElseStmt().isPresent()) {
                com.github.javaparser.ast.stmt.Statement elseStmt = current.getElseStmt().get();
                if (elseStmt instanceof IfStmt) {
                    current = (IfStmt) elseStmt;  // else if로 계속
                } else {
                    // else 블록 (조건 없음) - 마지막 브랜치
                    current = null;
                }
            } else {
                current = null;
            }
        }

        return chain;
    }

    /**
     * if-else-if 체인에 else-if가 하나라도 있는지 확인
     */
    private boolean hasElseIfInChain(IfStmt ifStmt) {
        // 첫 번째 if문의 else가 있는지 확인
        if (ifStmt.getElseStmt().isPresent()) {
            com.github.javaparser.ast.stmt.Statement elseStmt = ifStmt.getElseStmt().get();
            // else 문이 IfStmt이면 else-if 체인
            return elseStmt instanceof IfStmt;
        }
        return false;  // else가 없으면 단독 if문
    }

    /**
     * 조건식에서 변수와 값을 추출
     */
    private void extractConditions(Expression expr, ConditionInfo conditionInfo) {
        if (expr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;
            BinaryExpr.Operator operator = binaryExpr.getOperator();

            // 논리 연산자인 경우 (&&, ||)
            if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
                extractConditions(binaryExpr.getLeft(), conditionInfo);
                extractConditions(binaryExpr.getRight(), conditionInfo);
            } else {
                // 비교 연산자인 경우
                String left = binaryExpr.getLeft().toString();
                String right = binaryExpr.getRight().toString();
                String op = operator.asString();

                // 왼쪽이 변수, 오른쪽이 값인 경우
                if (isVariable(binaryExpr.getLeft()) && isLiteral(binaryExpr.getRight())) {
                    String type = inferType(binaryExpr.getRight());
                    conditionInfo.addVariableCondition(
                        new VariableCondition(left, op, right, type)
                    );
                }
                // 오른쪽이 변수, 왼쪽이 값인 경우
                else if (isLiteral(binaryExpr.getLeft()) && isVariable(binaryExpr.getRight())) {
                    String type = inferType(binaryExpr.getLeft());
                    String reversedOp = reverseOperator(op);
                    conditionInfo.addVariableCondition(
                        new VariableCondition(right, reversedOp, left, type)
                    );
                }
                // 두 변수를 비교하는 경우
                else if (isVariable(binaryExpr.getLeft()) && isVariable(binaryExpr.getRight())) {
                    // 왼쪽 표현식에서 타입 추론 시도
                    String type = inferTypeFromExpression(binaryExpr.getLeft());
                    conditionInfo.addVariableCondition(
                        new VariableCondition(left, op, right, type)
                    );
                }
            }
        } else if (expr instanceof UnaryExpr) {
            // ! (not) 연산자 처리
            UnaryExpr unaryExpr = (UnaryExpr) expr;
            if (unaryExpr.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                Expression innerExpr = unaryExpr.getExpression();
                if (isVariable(innerExpr)) {
                    conditionInfo.addVariableCondition(
                        new VariableCondition(innerExpr.toString(), "==", "false", "boolean")
                    );
                }
            }
        } else if (expr instanceof NameExpr || expr instanceof FieldAccessExpr) {
            // 단순 boolean 변수인 경우
            conditionInfo.addVariableCondition(
                new VariableCondition(expr.toString(), "==", "true", "boolean")
            );
        } else if (expr instanceof MethodCallExpr) {
            // 메서드 호출 결과를 조건으로 사용하는 경우
            MethodCallExpr methodCall = (MethodCallExpr) expr;
            conditionInfo.addVariableCondition(
                new VariableCondition(methodCall.toString(), "==", "true", "method")
            );
        } else if (expr instanceof EnclosedExpr) {
            // 괄호로 감싸진 표현식
            EnclosedExpr enclosedExpr = (EnclosedExpr) expr;
            extractConditions(enclosedExpr.getInner(), conditionInfo);
        }
    }

    /**
     * 표현식이 변수인지 확인
     * getter/setter 메서드 호출도 변수로 처리
     */
    private boolean isVariable(Expression expr) {
        return expr instanceof NameExpr ||
               expr instanceof FieldAccessExpr ||
               expr instanceof MethodCallExpr;  // getter/setter 지원
    }

    /**
     * 표현식이 리터럴 값인지 확인
     */
    private boolean isLiteral(Expression expr) {
        return expr instanceof LiteralExpr;
    }

    /**
     * 리터럴 값의 타입을 추론
     */
    private String inferType(Expression expr) {
        if (expr instanceof IntegerLiteralExpr) {
            return "int";
        } else if (expr instanceof DoubleLiteralExpr) {
            return "double";
        } else if (expr instanceof BooleanLiteralExpr) {
            return "boolean";
        } else if (expr instanceof StringLiteralExpr) {
            return "String";
        } else if (expr instanceof LongLiteralExpr) {
            return "long";
        } else if (expr instanceof CharLiteralExpr) {
            return "char";
        } else if (expr instanceof NullLiteralExpr) {
            return "null";
        }
        return "Object";
    }

    /**
     * Expression에서 타입을 추론 (변수, 메서드 호출 등)
     */
    private String inferTypeFromExpression(Expression expr) {
        // 리터럴인 경우
        if (expr instanceof LiteralExpr) {
            return inferType(expr);
        }

        // 메서드 호출인 경우 - 메서드 이름에서 타입 추론
        if (expr instanceof MethodCallExpr) {
            MethodCallExpr methodCall = (MethodCallExpr) expr;
            String methodName = methodCall.getNameAsString();

            // boolean 타입 메서드 (is*, has*, can*, should*)
            if (methodName.startsWith("is") ||
                methodName.startsWith("has") ||
                methodName.startsWith("can") ||
                methodName.startsWith("should")) {
                return "boolean";
            }

            // equals, contains 등의 메서드
            if (methodName.equals("equals") ||
                methodName.equals("contains") ||
                methodName.equals("isEmpty") ||
                methodName.equals("startsWith") ||
                methodName.equals("endsWith")) {
                return "boolean";
            }

            // length, size, count 등
            if (methodName.equals("length") ||
                methodName.equals("size") ||
                methodName.startsWith("count") ||
                methodName.startsWith("getCount")) {
                return "int";
            }

            // get으로 시작하는 메서드 - 일반적인 getter
            if (methodName.startsWith("get")) {
                // getName, getTitle 등 → String으로 추정
                if (methodName.contains("Name") ||
                    methodName.contains("Title") ||
                    methodName.contains("Message") ||
                    methodName.contains("Description")) {
                    return "String";
                }
                // getAge, getCount 등 → int로 추정
                if (methodName.contains("Age") ||
                    methodName.contains("Count") ||
                    methodName.contains("Size") ||
                    methodName.contains("Index")) {
                    return "int";
                }
                // getPrice, getAmount 등 → double로 추정
                if (methodName.contains("Price") ||
                    methodName.contains("Amount") ||
                    methodName.contains("Rate") ||
                    methodName.contains("Percent")) {
                    return "double";
                }
            }

            // 기본값
            return "Object";
        }

        // 일반 변수는 타입을 알 수 없음
        return "Object";
    }

    /**
     * 연산자를 역으로 변환 (좌우가 바뀐 경우)
     */
    private String reverseOperator(String operator) {
        switch (operator) {
            case ">": return "<";
            case "<": return ">";
            case ">=": return "<=";
            case "<=": return ">=";
            default: return operator;
        }
    }

    public Map<String, List<ConditionInfo>> getMethodConditions() {
        return methodConditions;
    }

    public Map<String, MethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public com.github.javaparser.ast.body.ConstructorDeclaration getPrimaryConstructor() {
        return primaryConstructor;
    }

    /**
     * 분석 결과를 출력
     */
    public void printAnalysisResult() {
        System.out.println("=== 분석 결과 ===");
        methodConditions.forEach((methodName, conditions) -> {
            System.out.println("\nMethod: " + methodName);
            conditions.forEach(condition -> {
                System.out.println("  Line " + condition.getLineNumber() + ": " + condition.getExpression());
                condition.getVariableConditions().forEach(vc -> {
                    System.out.println("    - Variable: " + vc.getVariableName() +
                                     " " + vc.getOperator() + " " + vc.getValue() +
                                     " (type: " + vc.getType() + ")");
                });
            });
        });
    }
}
