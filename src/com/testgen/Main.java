package com.testgen;

import com.testgen.analyzer.JavaSourceAnalyzer;
import com.testgen.generator.JUnitTestGenerator;
import com.testgen.model.ConditionInfo;
import com.testgen.model.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 메인 실행 클래스
 * Java 소스 디렉토리를 분석하여 JUnit 테스트 코드를 생성
 */
@Version("2.2")
public class Main {
    public static void main(String[] args) {
        // 콘솔 인코딩을 UTF-8로 강제 설정
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // UTF-8을 지원하지 않는 환경은 거의 없음
        }

        // --version 또는 -v 옵션 처리
        if (args.length > 0 && (args[0].equals("--version") || args[0].equals("-v"))) {
            printVersion();
            return;
        }

        if (args.length < 2) {
            System.out.println("TestGenerator v" + getVersion());
            System.out.println("\nUsage: java Main <source-path> <test-directory>");
            System.out.println("       java Main --version | -v");
            System.out.println("\nExample: java Main src/main/java src/test/java");
            System.out.println("Example: java Main src/main/java/com/example/Calculator.java src/test/java");
            System.out.println("\n설명:");
            System.out.println("  <source-path>: Java 소스 파일 또는 디렉토리");
            System.out.println("                 - 파일: 해당 파일만 처리");
            System.out.println("                 - 디렉토리: 하위 디렉토리 포함 모든 Java 파일 처리");
            System.out.println("  <test-directory>: JUnit 테스트 파일을 생성할 디렉토리");
            return;
        }

        String sourceDir = args[0];
        String testDir = args[1];

        try {
            File sourceFile = new File(sourceDir);

            if (!sourceFile.exists()) {
                System.err.println("소스 파일/디렉토리가 존재하지 않습니다: " + sourceDir);
                return;
            }

            System.out.println("=== TestGenerator v" + getVersion() + " 시작 ===");
            System.out.println("테스트 디렉토리: " + new File(testDir).getAbsolutePath());
            System.out.println();

            List<File> javaFiles = new ArrayList<>();
            File sourceRoot;

            // 파일인지 디렉토리인지 확인
            if (sourceFile.isFile()) {
                // 단일 파일 처리
                if (!sourceFile.getName().endsWith(".java")) {
                    System.err.println("Java 파일이 아닙니다: " + sourceDir);
                    return;
                }
                System.out.println("단일 파일 모드");
                System.out.println("소스 파일: " + sourceFile.getAbsolutePath());
                javaFiles.add(sourceFile);
                sourceRoot = sourceFile.getParentFile();
            } else if (sourceFile.isDirectory()) {
                // 디렉토리 전체 처리
                System.out.println("디렉토리 모드");
                System.out.println("소스 디렉토리: " + sourceFile.getAbsolutePath());
                javaFiles = findAllJavaFiles(sourceFile);
                sourceRoot = sourceFile;
            } else {
                System.err.println("올바른 파일 또는 디렉토리가 아닙니다: " + sourceDir);
                return;
            }

            System.out.println("발견된 Java 파일: " + javaFiles.size() + "개\n");

            int successCount = 0;
            int failCount = 0;

            for (File javaFile : javaFiles) {
                try {
                    System.out.println("처리 중: " + javaFile.getName());
                    processJavaFile(javaFile, sourceRoot, testDir);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("  오류 발생: " + e.getMessage());
                    failCount++;
                }
            }

            System.out.println("\n=== 완료 ===");
            System.out.println("성공: " + successCount + "개");
            System.out.println("실패: " + failCount + "개");

        } catch (Exception e) {
            System.err.println("예기치 않은 오류 발생");
            e.printStackTrace();
        }
    }

    /**
     * 디렉토리에서 모든 .java 파일을 재귀적으로 찾기
     */
    private static List<File> findAllJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(findAllJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    /**
     * 개별 Java 파일 처리
     */
    private static void processJavaFile(File javaFile, File sourceRoot, String testDir) throws IOException {
        // 1. Java 소스 파일 분석
        JavaSourceAnalyzer analyzer = new JavaSourceAnalyzer();
        analyzer.analyzeFile(javaFile.getAbsolutePath());

        Map<String, List<ConditionInfo>> methodConditions = analyzer.getMethodConditions();

        // 조건이 없는 파일은 건너뛰기
        if (methodConditions.isEmpty()) {
            System.out.println("  건너뛰기: 분석할 if문이 없습니다.");
            return;
        }

        // 2. 패키지명과 클래스명 추출
        String packageName = analyzer.getPackageName();
        String className = analyzer.getClassName();

        if (className.isEmpty()) {
            System.out.println("  건너뛰기: 클래스를 찾을 수 없습니다.");
            return;
        }

        // 3. 테스트 파일 경로 계산 (패키지 구조 반영)
        String packagePath = packageName.replace('.', File.separatorChar);
        String testFileName = className + "Test.java";
        String testFilePath = testDir + File.separator + packagePath + File.separator + testFileName;

        // 테스트 디렉토리 생성
        File testFile = new File(testFilePath);
        testFile.getParentFile().mkdirs();

        // 4. 테스트 케이스 생성
        JUnitTestGenerator generator = new JUnitTestGenerator(packageName, className);
        Map<String, List<TestCase>> allTestCases = new HashMap<>();

        for (Map.Entry<String, List<ConditionInfo>> entry : methodConditions.entrySet()) {
            String methodName = entry.getKey();
            List<ConditionInfo> conditions = entry.getValue();
            List<TestCase> testCases = generator.generateTestCases(conditions);
            allTestCases.put(methodName, testCases);
        }

        // 5. 테스트 코드 파일 생성 (메서드 정보 및 생성자 포함)
        generator.generateTestCode(allTestCases, analyzer.getMethodDeclarations(),
                                   analyzer.getPrimaryConstructor(), testFilePath);
        System.out.println("  생성 완료: " + testFilePath);
    }

    /**
     * 프로그래매틱하게 단일 파일을 처리하는 메서드
     * (레거시 호환성을 위해 유지)
     */
    public static void generateTestForFile(
            String sourceFilePath,
            String outputPath) throws IOException {

        JavaSourceAnalyzer analyzer = new JavaSourceAnalyzer();
        analyzer.analyzeFile(sourceFilePath);

        String packageName = analyzer.getPackageName();
        String className = analyzer.getClassName();

        if (className.isEmpty()) {
            throw new RuntimeException("클래스를 찾을 수 없습니다: " + sourceFilePath);
        }

        JUnitTestGenerator generator = new JUnitTestGenerator(packageName, className);
        Map<String, List<ConditionInfo>> methodConditions = analyzer.getMethodConditions();
        Map<String, List<TestCase>> allTestCases = new HashMap<>();

        for (Map.Entry<String, List<ConditionInfo>> entry : methodConditions.entrySet()) {
            String methodName = entry.getKey();
            List<ConditionInfo> conditions = entry.getValue();
            List<TestCase> testCases = generator.generateTestCases(conditions);
            allTestCases.put(methodName, testCases);
        }

        generator.generateTestCode(allTestCases, analyzer.getMethodDeclarations(),
                                   analyzer.getPrimaryConstructor(), outputPath);
    }

    /**
     * 버전 정보를 가져오기 (Maven properties 파일에서 읽기)
     */
    public static String getVersion() {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                return prop.getProperty("version", "unknown");
            }
        } catch (IOException e) {
            // properties 파일을 읽을 수 없는 경우
        }

        // properties 파일이 없으면 annotation에서 읽기
        Version versionAnnotation = Main.class.getAnnotation(Version.class);
        if (versionAnnotation != null) {
            return versionAnnotation.value();
        }

        // Package의 Implementation-Version 확인 (JAR에서 실행할 때)
        Package pkg = Main.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }

        return "unknown";
    }

    /**
     * 버전 정보 출력
     */
    public static void printVersion() {
        String version = getVersion();
        System.out.println("TestGenerator version " + version);

        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String artifactId = prop.getProperty("artifactId", "unknown");
                String groupId = prop.getProperty("groupId", "unknown");
                System.out.println("Artifact: " + groupId + ":" + artifactId);
            }
        } catch (IOException e) {
            // properties 파일을 읽을 수 없는 경우 무시
        }

        Package pkg = Main.class.getPackage();
        if (pkg != null && pkg.getImplementationTitle() != null) {
            System.out.println("Implementation: " + pkg.getImplementationTitle());
        }
    }
}
