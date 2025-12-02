package com.testgen.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import com.testgen.Main;
import com.testgen.analyzer.JavaSourceAnalyzer;
import com.testgen.generator.JUnitTestGenerator;
import com.testgen.model.ConditionInfo;
import com.testgen.model.TestCase;

/**
 * 웹 인터페이스를 통한 테스트 생성 컨트롤러
 */
public class TestGeneratorController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setAttribute("version", Main.getVersion());
		req.getRequestDispatcher("/index.jsp").forward(req, res);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String action = req.getParameter("action");
		System.out.println("action:" + action);
		if ("generateTest".equals(action)) {
			System.out.println("generateTest 호출됨");
			generateTest(req, res);
		} else if ("runTest".equals(action)) {
			runTest(req, res);
		} else {
			doGet(req, res);
		}
	}

	private void generateTest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String sourcePath = req.getParameter("sourcePath");
		String testPath = req.getParameter("testPath");
		if (sourcePath != null) {
			sourcePath = sourcePath.trim();
		}
		if (testPath != null) {
			testPath = testPath.trim();
		}
		try {
			File sourceFile = new File(sourcePath);
			if (!sourceFile.exists()) {
				req.setAttribute("error", "소스 파일/디렉토리가 존재하지 않습니다: " + sourcePath);
				req.setAttribute("version", Main.getVersion());
				req.getRequestDispatcher("/index.jsp").forward(req, res);
				return;
			}

			List<File> javaFiles = new ArrayList<>();
			File sourceRoot;
			// 파일인지 디렉토리인지 확인
			if (sourceFile.isFile()) {
				if (!sourceFile.getName().endsWith(".java")) {
					req.setAttribute("error", "Java 파일이 아닙니다: " + sourcePath);
					req.setAttribute("version", Main.getVersion());
					req.getRequestDispatcher("/index.jsp").forward(req, res);
					return;
				}
				javaFiles.add(sourceFile);
				sourceRoot = sourceFile.getParentFile();
			} else if (sourceFile.isDirectory()) {
				javaFiles = findAllJavaFiles(sourceFile);
				sourceRoot = sourceFile;
			} else {
				req.setAttribute("error", "올바른 파일 또는 디렉토리가 아닙니다: " + sourcePath);
				req.setAttribute("version", Main.getVersion());
				req.getRequestDispatcher("/index.jsp").forward(req, res);
				return;
			}
			int successCount = 0;
			int failCount = 0;
			List<String> results = new ArrayList<>();

			for (File javaFile : javaFiles) {
				try {
					processJavaFile(javaFile, sourceRoot, testPath);
					successCount++;
					results.add("성공: " + javaFile.getName());
				} catch (Exception e) {
					failCount++;
					results.add("실패: " + javaFile.getName() + " - " + e.getMessage());
				}
			}
			req.setAttribute("success", true);
			req.setAttribute("successCount", successCount);
			req.setAttribute("failCount", failCount);
			req.setAttribute("results", results);
			req.setAttribute("testPath", new File(testPath).getAbsolutePath());
			req.setAttribute("version", Main.getVersion());

		} catch (Exception e) {
			req.setAttribute("error", "오류 발생: " + e.getMessage());
			req.setAttribute("version", Main.getVersion());
		}

		req.getRequestDispatcher("/index.jsp").forward(req, res);
	}

	/**
	 * 디렉토리에서 모든 .java 파일을 재귀적으로 찾기
	 */
	private List<File> findAllJavaFiles(File directory) {
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
	private void processJavaFile(File javaFile, File sourceRoot, String testDir) throws IOException {
		JavaSourceAnalyzer analyzer = new JavaSourceAnalyzer();
		analyzer.analyzeFile(javaFile.getAbsolutePath());

		Map<String, List<ConditionInfo>> methodConditions = analyzer.getMethodConditions();

		if (methodConditions.isEmpty()) {
			throw new RuntimeException("분석할 if문이 없습니다.");
		}

		String packageName = analyzer.getPackageName();
		String className = analyzer.getClassName();

		if (className.isEmpty()) {
			throw new RuntimeException("클래스를 찾을 수 없습니다.");
		}

		String packagePath = packageName.replace('.', File.separatorChar);
		String testFileName = className + "Test.java";
		String testFilePath = testDir + File.separator + packagePath + File.separator + testFileName;

		File testFile = new File(testFilePath);
		testFile.getParentFile().mkdirs();

		JUnitTestGenerator generator = new JUnitTestGenerator(packageName, className);
		Map<String, List<TestCase>> allTestCases = new HashMap<>();

		for (Map.Entry<String, List<ConditionInfo>> entry : methodConditions.entrySet()) {
			String methodName = entry.getKey();
			List<ConditionInfo> conditions = entry.getValue();
			List<TestCase> testCases = generator.generateTestCases(conditions);
			allTestCases.put(methodName, testCases);
		}

		generator.generateTestCode(allTestCases, analyzer.getMethodDeclarations(), analyzer.getPrimaryConstructor(), testFilePath);
	}

	private void runTest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String testPath = req.getParameter("testPath");
		try {
			File testDir = new File(testPath);
			if (!testDir.exists() || !testDir.isDirectory()) {
				req.setAttribute("error", "테스트 디렉토리가 존재하지 않습니다: " + testPath);
				req.setAttribute("version", Main.getVersion());
				req.getRequestDispatcher("/index.jsp").forward(req, res);
				return;
			}
			// 테스트 클래스 찾기
			List<String> testClasses = findTestClasses(testDir, testPath);

			if (testClasses.isEmpty()) {
				req.setAttribute("error", "실행할 테스트 파일이 없습니다.");
				req.setAttribute("version", Main.getVersion());
				req.getRequestDispatcher("/index.jsp").forward(req, res);
				return;
			}
			// 테스트 실행
			List<String> testResults = new ArrayList<>();
			int totalTests = 0;
			int passedTests = 0;
			int failedTests = 0;

			for (String testClass : testClasses) {
				try {
					TestExecutionSummary summary = executeTest(testClass);
					
					// summary가 null인지 확인
					if (summary == null) {
						testResults.add("실패: " + testClass + " - 테스트 실행 결과를 가져올 수 없습니다.");
						continue;
					}
					
					long tests = summary.getTestsSucceededCount() + summary.getTestsFailedCount();
					long passed = summary.getTestsSucceededCount();
					long failed = summary.getTestsFailedCount();

					totalTests += tests;
					passedTests += passed;
					failedTests += failed;
					System.out.println("tests:" + tests + ", passed:" + passed + ", failed:" + failed);
					String className = testClass.substring(testClass.lastIndexOf('.') + 1);

					testResults.add(String.format("%s: %d개 테스트 (성공: %d, 실패: %d)", className, tests, passed, failed));

				} catch (Exception e) {
					testResults.add("실패: " + testClass + " - " + e.getMessage());
					e.printStackTrace();
				}
			}
			req.setAttribute("testRunSuccess", true);
			req.setAttribute("totalTests", totalTests);
			req.setAttribute("passedTests", passedTests);
			req.setAttribute("failedTests", failedTests);
			req.setAttribute("testResults", testResults);
			req.setAttribute("version", Main.getVersion());

		} catch (Exception e) {
			req.setAttribute("error", "테스트 실행 오류: " + e.getMessage());
			req.setAttribute("version", Main.getVersion());
		}
		req.getRequestDispatcher("/index.jsp").forward(req, res);
	}

	/**
	 * 테스트 디렉토리에서 모든 테스트 클래스 찾기
	 */
	private List<String> findTestClasses(File dir, String basePath) {
		List<String> testClasses = new ArrayList<>();
		File[] files = dir.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					testClasses.addAll(findTestClasses(file, basePath));
				} else if (file.getName().endsWith("Test.java")) {
					// 파일 경로를 클래스명으로 변환
					String relativePath = file.getAbsolutePath().substring(basePath.length() + 1);
					String className = relativePath.replace(File.separatorChar, '.').replace(".java", "");
					testClasses.add(className);
				}
			}
		}

		return testClasses;
	}

	/**
	 * JUnit 테스트 실행
	 */
	private TestExecutionSummary executeTest(String testClassName) throws Exception {
		// 테스트 클래스 로드를 위한 ClassLoader 생성
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		try {
			// 서블릿 컨텍스트의 실제 경로 가져오기
			String realPath = getServletContext().getRealPath("/WEB-INF/classes");
			File testClassesDir = new File(realPath);
			System.out.println("testClassName:" + testClassName);
			System.out.println("testClassesDir (real path): " + testClassesDir.getAbsolutePath());

			// WEB-INF/classes가 존재하는지 확인
			if (!testClassesDir.exists()) {
				System.err.println("Error: testClassesDir does not exist: " + testClassesDir.getAbsolutePath());
				throw new RuntimeException("테스트 클래스 디렉토리가 존재하지 않습니다: " + testClassesDir.getAbsolutePath());
			}

			URL testClassesUrl = testClassesDir.toURI().toURL();

			// WEB-INF/lib의 모든 JAR 파일도 classpath에 추가
			String libPath = getServletContext().getRealPath("/WEB-INF/lib");
			File libDir = new File(libPath);
			System.out.println("libDir (real path): " + libDir.getAbsolutePath());
			java.util.List<URL> urls = new ArrayList<>();
			urls.add(testClassesUrl);

			if (libDir.exists() && libDir.isDirectory()) {
				File[] jarFiles = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
				if (jarFiles != null) {
					for (File jarFile : jarFiles) {
						urls.add(jarFile.toURI().toURL());
					}
				}
			}

			URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), originalClassLoader);

			// ClassLoader 컨텍스트를 먼저 설정 (중요!)
			Thread.currentThread().setContextClassLoader(classLoader);
			
			// 테스트 클래스 로드
			Class<?> testClass;
			try {
				testClass = classLoader.loadClass(testClassName);
				System.out.println("Successfully loaded test class: " + testClassName);
				System.out.println("Test class ClassLoader: " + testClass.getClassLoader());
				
				// 클래스의 메서드를 확인하여 @Test 어노테이션이 있는지 검증
				java.lang.reflect.Method[] methods = testClass.getDeclaredMethods();
				int testMethodCount = 0;
				for (java.lang.reflect.Method method : methods) {
					if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
						testMethodCount++;
						System.out.println("  Found @Test method: " + method.getName());
					}
				}
				System.out.println("Total @Test methods found: " + testMethodCount);
				
				if (testMethodCount == 0) {
					System.err.println("WARNING: No @Test methods found in class: " + testClassName);
					throw new RuntimeException("테스트 메서드가 없습니다: " + testClassName);
				}
				
			} catch (ClassNotFoundException e) {
				System.err.println("Failed to load test class: " + testClassName);
				System.err.println("ClassNotFoundException: " + e.getMessage());
				throw new RuntimeException("테스트 클래스를 찾을 수 없습니다: " + testClassName + 
					". 테스트 파일이 컴파일되었는지 확인하세요.", e);
			}

			// JUnit Platform Launcher 설정 (ClassLoader 컨텍스트가 이미 설정된 상태에서)
			System.out.println("Creating LauncherDiscoveryRequest for class: " + testClassName);
			LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(DiscoverySelectors.selectClass(testClass))
				.build();
			
			System.out.println("Creating Launcher with custom ClassLoader...");
			// LauncherFactory가 현재 스레드의 ClassLoader를 사용하도록 함
			Launcher launcher = LauncherFactory.create();

			System.out.println("Registering test execution listener...");
			launcher.registerTestExecutionListeners(listener);
			
			System.out.println("Executing tests...");
			System.out.println("Current thread ClassLoader: " + Thread.currentThread().getContextClassLoader());
			launcher.execute(request);
			
			System.out.println("Test execution completed.");
			TestExecutionSummary summary = listener.getSummary();
			if (summary != null) {
				System.out.println("Tests found: " + (summary.getTestsSucceededCount() + summary.getTestsFailedCount()));
				System.out.println("Tests succeeded: " + summary.getTestsSucceededCount());
				System.out.println("Tests failed: " + summary.getTestsFailedCount());
			} else {
				System.err.println("WARNING: Test execution summary is null");
			}

			return summary;
		} catch (Exception e) {
			System.err.println("Error executing test: " + e.getMessage());
			e.printStackTrace();
			// 예외 발생 시 null을 반환하여 호출자가 처리하도록 함
			return null;
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}
}
