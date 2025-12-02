# ? 빠른 해결: tests:0 문제

## 문제
```
tests:0, passed:0, failed:0
```
테스트 클래스는 있지만 실행되지 않음

## 원인
? **JUnit 버전 불일치**
- JUnit Jupiter: 5.11.0 (최신)
- JUnit Platform: 1.8.0 (구버전) ← 문제!

## 해결 (5분)

### 1단계: 다운로드 (2분)
다음 2개 파일을 브라우저에서 다운로드:

**파일 1**: junit-platform-engine-1.11.0.jar
→ https://repo1.maven.org/maven2/org/junit/platform/junit-platform-engine/1.11.0/junit-platform-engine-1.11.0.jar

**파일 2**: junit-platform-launcher-1.11.0.jar  
→ https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.11.0/junit-platform-launcher-1.11.0.jar

### 2단계: 설치 (1분)
```
1. 다운로드한 2개 파일을 복사:
   → D:\GitHub\GenerateTest\GenerateTest\webapp\WEB-INF\lib\

2. 구버전 삭제:
   ? junit-platform-engine-1.8.0.jar
   ? junit-platform-launcher-1.8.0.jar
```

### 3단계: 재시작 (2분)
```
1. Eclipse: Project → Clean → OK
2. Eclipse: Project → Build Project
3. Tomcat: 서버 탭에서 우클릭 → Restart
```

### 4단계: 테스트
```
웹 페이지에서 "테스트 실행" 클릭
→ ? tests:20, passed:20, failed:0
```

## 완료!

---

**자세한 내용**: 최종진단_테스트0개문제.md 참고
