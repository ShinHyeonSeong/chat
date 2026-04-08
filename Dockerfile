# ============================================================
# Dockerfile — Spring Boot 채팅 애플리케이션
# ============================================================
#
# [Docker 이미지란?]
#   이미지는 애플리케이션 실행에 필요한 모든 것(OS, JDK, JAR 등)을
#   하나로 묶은 "스냅샷"입니다. 컨테이너는 이 이미지를 실행한 인스턴스입니다.
#
# [멀티 스테이지 빌드란?]
#   FROM ... AS builder  ← 1단계: 빌드 환경 (Gradle + JDK)
#   FROM ...             ← 2단계: 실행 환경 (JRE만)
#
#   1단계에서 만든 WAR 파일만 2단계로 복사합니다.
#   덕분에 최종 이미지에는 Gradle, 소스코드 등이 포함되지 않아
#   이미지 크기가 훨씬 작아집니다.
# ============================================================


# ============================================================
# Stage 1 : 빌드 단계
# ============================================================
# eclipse-temurin:17-jdk-alpine
#   - eclipse-temurin: OpenJDK의 공식 배포판 (AdoptOpenJDK 후신)
#   - 17: Java 17 버전
#   - jdk: 컴파일러 포함 (빌드에 필요)
#   - alpine: 매우 가벼운 Linux 배포판 (이미지 크기 축소)
#
# AS builder: 이 단계에 "builder"라는 이름을 붙입니다.
#             2단계에서 이 이름으로 파일을 참조합니다.
# ============================================================
FROM eclipse-temurin:17-jdk-alpine AS builder

# WORKDIR: 컨테이너 안에서 작업할 "현재 디렉터리"를 설정합니다.
# 이후 COPY, RUN 명령어는 모두 이 경로 기준으로 실행됩니다.
WORKDIR /app

# ── 의존성 캐싱 최적화 ──────────────────────────────────────
# [왜 소스코드보다 먼저 복사할까요?]
#   Docker는 각 명령어의 결과를 "레이어"로 캐싱합니다.
#   파일이 변경되지 않았으면 이전에 캐싱된 레이어를 재사용합니다.
#
#   빌드 설정 파일(build.gradle 등)은 소스코드보다 변경이 드뭅니다.
#   이 파일들만 먼저 복사하고 의존성을 다운로드해 두면,
#   소스코드만 수정한 경우 의존성 다운로드를 건너뛸 수 있어
#   빌드 속도가 크게 향상됩니다.
# ───────────────────────────────────────────────────────────
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# Gradle Wrapper에 실행 권한을 부여합니다.
# Windows에서 복사된 파일은 Linux에서 실행 권한이 없을 수 있습니다.
RUN chmod +x ./gradlew

# 의존성만 먼저 다운로드합니다. (소스코드 변경 시 이 레이어는 캐시 재사용)
RUN ./gradlew dependencies --no-daemon

# 나머지 소스 코드를 복사합니다.
COPY src ./src

# WAR 파일을 빌드합니다.
#   bootWar: Spring Boot 실행 가능한 WAR 파일 생성
#   -x test: 테스트를 건너뜁니다 (빌드 속도 향상)
#   --no-daemon: CI/컨테이너 환경에서는 Gradle 데몬 비활성화 권장
RUN ./gradlew bootWar -x test --no-daemon


# ============================================================
# Stage 2 : 실행 단계
# ============================================================
# eclipse-temurin:17-jre-alpine
#   - jre: 실행 전용 (JDK보다 훨씬 가벼움, 컴파일러 불필요)
#   - alpine: 가벼운 Linux
#
# 빌드 도구(Gradle, JDK 컴파일러 등)는 이 이미지에 포함되지 않습니다.
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# --from=builder: "builder" 단계의 파일시스템에서 복사합니다.
# build/libs/*.war: Gradle이 생성한 WAR 파일 (버전 포함된 이름을 와일드카드로 처리)
# app.war: 컨테이너 안에 저장될 파일 이름 (단순하게 고정)
COPY --from=builder /app/build/libs/*.war app.war

# EXPOSE: 이 컨테이너가 8003 포트를 사용한다고 "문서화"합니다.
# 실제로 포트를 여는 건 docker-compose.yml의 ports 설정입니다.
EXPOSE 8003

# ENTRYPOINT: 컨테이너가 시작될 때 실행할 명령어입니다.
# Spring Boot의 bootWar는 java -jar 로 직접 실행 가능합니다.
#
# SPRING_PROFILES_ACTIVE=docker 환경변수는 docker-compose.yml에서 주입됩니다.
# → src/main/resources/application-docker.yml 설정이 활성화됩니다.
ENTRYPOINT ["java", "-jar", "app.war"]