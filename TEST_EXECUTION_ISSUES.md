# 테스트 실행 문제 분석 및 해결 방안

## 발견된 문제점

### 1. **테스트 클래스 컴파일 문제**
**문제**: 테스트 파일이 `src` 디렉토리에 생성되지만, 자동으로 컴파일되지 않습니다.
- 테스트 생성: `D:\GitHub\GenerateTest\GenerateTest\src\com\example\CalculatorTest.java`
- 테스트 실행: `webapp/WEB-INF/classes`에서 `.class` 파일을 찾으려고 시도
- 결과: `ClassNotFoundException` 발생

**해결 방법**:
테스트 파일을 생성한 후, Eclipse에서 자동 컴파일이 되도록 하거나, 수동으로 컴파일해야 합니다.

```bash
# 방법 1: Eclipse에서 프로젝트 빌드
Project → Build Project (또는 Ctrl+B)

# 방법 2: 명령줄에서 컴파일
javac -cp "webapp/WEB-INF/lib/*" -d webapp/WEB-INF/classes src/com/example/CalculatorTest.java
```

### 2. **@AfterAll 메서드 접근 제한자 문제**
**문제**: 생성된 테스트 코드의 `@AfterAll` 메서드가 `public` 접근 제한자가 없습니다.
```java
@AfterAll
static void printTestSummary() {  // ? public이 없음
```

**해결**: JUnit 5는 `@AfterAll` 메서드가 `public static`이어야 합니다.
```java
@AfterAll
public static void printTestSummary() {  // ? public 추가
```

이 문제는 `JUnitTestGenerator.java`에서 수정되었습니다.

### 3. **ClassLoader 설정 문제**
**문제**: 원래 코드는 JUnit 라이브러리를 포함한 모든 의존성을 ClassLoader에 추가하지 않았습니다.

**해결**: `TestGeneratorController.java`의 `executeTest` 메서드를 수정하여:
- `webapp/WEB-INF/lib`의 모든 JAR 파일을 ClassLoader에 추가
- 더 나은 오류 메시지 및 로깅 추가
- `ClassNotFoundException` 발생 시 명확한 오류 메시지 제공

### 4. **테스트 파일 경로 문제**
**문제**: `findTestClasses` 메서드가 테스트 소스 디렉토리를 스캔하지만, 컴파일된 클래스는 다른 위치에 있습니다.

**현재 동작**:
1. 테스트 생성: `src/com/example/CalculatorTest.java`
2. 테스트 찾기: `src` 디렉토리 스캔 → `com.example.CalculatorTest` 찾음
3. 테스트 실행: `webapp/WEB-INF/classes/com/example/CalculatorTest.class` 로드 시도
4. 파일이 없으면 실패

## 수정된 내용

### 1. TestGeneratorController.java
- `executeTest` 메서드 개선:
  - WEB-INF/lib의 모든 JAR 파일을 ClassLoader에 추가
  - 더 나은 오류 처리 및 로깅
  - ClassNotFoundException 발생 시 명확한 오류 메시지

### 2. JUnitTestGenerator.java
- `@AfterAll` 메서드에 `public` 접근 제한자 추가
- 생성된 모든 테스트에서 JUnit 5 규칙 준수

## 테스트 실행 방법

### 전체 워크플로우:

1. **테스트 생성**:
   - 웹 인터페이스에서 소스 파일 경로와 테스트 디렉토리 입력
   - "테스트 생성" 버튼 클릭
   - 테스트 파일이 `src` 디렉토리에 생성됨

2. **테스트 컴파일** (중요!):
   - Eclipse에서 자동 빌드가 활성화되어 있는지 확인:
     `Project → Build Automatically` 체크
   - 또는 수동으로 프로젝트 빌드: `Project → Build Project`
   - 컴파일된 `.class` 파일이 `webapp/WEB-INF/classes`에 생성됨

3. **테스트 실행**:
   - 웹 인터페이스에서 "테스트 실행" 버튼 클릭
   - JUnit 5가 테스트를 실행하고 결과 표시

### Eclipse 설정 확인:

1. **자동 빌드 확인**:
   - `Project → Build Automatically`가 체크되어 있는지 확인

2. **Build Path 확인**:
   - 프로젝트 우클릭 → Properties → Java Build Path
   - Source 탭에서 출력 폴더가 `webapp/WEB-INF/classes`인지 확인

3. **JUnit 라이브러리 확인**:
   - `webapp/WEB-INF/lib`에 다음 JAR 파일들이 있는지 확인:
     - junit-jupiter-api-5.11.0.jar
     - junit-jupiter-engine-5.11.0.jar
     - junit-platform-launcher-1.8.0.jar
     - junit-platform-engine-1.8.0.jar
     - junit-platform-commons-1.11.0.jar

## 여전히 발생 가능한 문제

### 문제: "테스트 클래스를 찾을 수 없습니다"
**원인**: 테스트 파일이 컴파일되지 않았습니다.
**해결**: Eclipse에서 프로젝트를 빌드하거나 자동 빌드를 활성화합니다.

### 문제: "NoClassDefFoundError: org/junit/jupiter/api/Test"
**원인**: JUnit 라이브러리가 ClassLoader에 로드되지 않았습니다.
**해결**: 이미 수정된 코드에서는 자동으로 WEB-INF/lib의 모든 JAR을 로드합니다.

### 문제: 테스트가 0개 실행됨
**원인**: 
1. 테스트 메서드가 제대로 생성되지 않았거나
2. @Test 어노테이션이 누락되었거나
3. 테스트 클래스 로딩 실패

**해결**: 
1. 생성된 테스트 파일을 직접 열어서 @Test 어노테이션이 있는지 확인
2. 콘솔 로그를 확인하여 자세한 오류 메시지 확인

## 개선 제안

### 단기 개선 (즉시 적용 가능):
1. ? @AfterAll 메서드에 public 접근 제한자 추가 (완료)
2. ? ClassLoader에 WEB-INF/lib JAR 파일들 추가 (완료)
3. ? 더 나은 오류 메시지 추가 (완료)

### 장기 개선 (추가 작업 필요):
1. **자동 컴파일 기능 추가**:
   - 테스트 생성 후 JavaCompiler API를 사용하여 자동으로 컴파일
   
2. **테스트 실행 전 유효성 검사**:
   - .class 파일 존재 여부 확인
   - 컴파일되지 않은 경우 사용자에게 안내 메시지 표시

3. **별도의 테스트 디렉토리 구조**:
   - `src/main/java`: 소스 코드
   - `src/test/java`: 테스트 코드
   - Maven/Gradle 같은 표준 프로젝트 구조 사용

## 결론

주요 문제는 **테스트 파일 생성과 컴파일이 분리되어 있다**는 점입니다.

**현재 워크플로우**:
1. 테스트 생성 (소스 파일만)
2. ? 컴파일 누락 ?
3. 테스트 실행 시도 → 실패

**수정된 워크플로우**:
1. 테스트 생성 (소스 파일)
2. ? Eclipse 자동 빌드 또는 수동 컴파일 ?
3. 테스트 실행 → 성공

코드 수정으로 오류 처리와 ClassLoader 설정은 개선되었지만, **Eclipse에서 자동 빌드가 활성화되어 있거나 수동으로 빌드를 실행해야** 테스트가 정상적으로 작동합니다.
