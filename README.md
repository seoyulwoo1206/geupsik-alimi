# 급식 알리미

우리 학교 급식을 보여주고, 매일 정해진 시간에 오늘 메뉴를 알려주는 안드로이드 앱.
데이터는 [나이스 교육정보 개방 포털](https://open.neis.go.kr)의 학교기본정보·급식식단정보 API를 씁니다.

## 기능

- 학교 검색·등록 (주소 함께 표시)
- 오늘 급식, 좌우로 날짜 이동, 일주일치 캐시 (인터넷 없어도 보임)
- 알레르기 19종 등록 → 해당 메뉴 빨간색 + 원인 표시
- 매일 정한 시간에 오늘 메뉴 알림 (정확한 알람 권한 없이 동작, 재부팅 후 유지)
- 깃허브 최신 릴리스 확인 후 새 버전 안내 (github 빌드만)

## 개발 환경

- Android Studio (JDK는 Android Studio에 들어 있는 JBR 사용)
- Android SDK Platform 37
- Kotlin + Jetpack Compose

## 처음 설정

1. `local.properties.example`을 `local.properties`로 복사합니다. Android Studio로 열면 `sdk.dir`은 자동으로 채워집니다.
2. 나이스 포털에서 인증키를 발급받아 `NEIS_API_KEY=` 뒤에 넣습니다.
   키를 비워 두면 샘플 모드로 동작합니다 (검색 결과 최대 5개).
3. `gradle.properties`의 `githubRepo=`에 깃허브 저장소를 `아이디/저장소이름` 형식으로 넣습니다.
   비워 두면 업데이트 확인을 하지 않습니다.

`local.properties`, `keystore.properties`, `*.jks`는 `.gitignore`에 들어 있어서 깃허브에 올라가지 않습니다.

## 빌드 종류

| 빌드 | 용도 | 업데이트 확인 |
|---|---|---|
| `githubDebug` | 개발·테스트 | O |
| `githubRelease` | 깃허브 릴리스로 배포할 APK | O |
| `playRelease` | 플레이스토어용 (AAB) | X (스토어 정책상 스토어 밖 업데이트 안내 금지) |

```bash
./gradlew assembleGithubDebug
```

APK 위치: `app/build/outputs/apk/github/debug/app-github-debug.apk`

## 릴리스 서명 키 만들기 (처음 한 번)

릴리스 APK는 직접 만든 키로 서명해야 합니다.
**이 키 파일과 비밀번호를 잃어버리면, 친구들 폰에 깔린 앱을 업데이트할 수 없습니다.**
(새 키로 만든 APK는 기존 앱을 지워야 설치됨) 프로젝트 폴더 말고 다른 곳에도 꼭 백업해 두세요.

1. 프로젝트 폴더에서 키를 만듭니다. 비밀번호는 직접 정해서 입력합니다.

   ```bash
   "/c/Program Files/Android/Android Studio/jbr/bin/keytool" -genkeypair -v -keystore release.jks -alias geupsik -keyalg RSA -keysize 2048 -validity 10000
   ```

2. `keystore.properties.example`을 `keystore.properties`로 복사하고 비밀번호를 채웁니다.

## 새 버전 배포 (깃허브 릴리스)

1. `app/build.gradle.kts`에서 `versionCode`를 1 올리고 `versionName`을 바꿉니다. (예: `0.2.0`)
2. 릴리스 APK를 만듭니다.

   ```bash
   ./gradlew assembleGithubRelease
   ```

   결과: `app/build/outputs/apk/github/release/app-github-release.apk`
3. 깃허브 저장소 → Releases → Draft a new release
   - 태그: `v0.2.0` (versionName 앞에 `v`)
   - 변경 내용 작성 (앱의 업데이트 안내 팝업에 그대로 보입니다)
   - APK 파일 첨부 → Publish

앱은 켤 때 최신 릴리스 태그와 자기 버전을 비교해서, 새 버전이면 APK 다운로드 링크를 안내합니다.
같은 와이파이를 쓰는 친구들이 많으면 깃허브 호출 한도(IP당 시간당 60번)에 걸릴 수 있어서 6시간에 한 번만 확인합니다.

## 플레이스토어

```bash
./gradlew bundlePlayRelease
```

플레이스토어는 "Play 앱 서명"으로 구글이 다시 서명하기 때문에, 깃허브 APK로 설치한 앱과 스토어 앱은 서로 업데이트되지 않습니다.
스토어에 올린 뒤에는 친구들에게 기존 앱을 지우고 스토어에서 받도록 안내하세요.

## 구조

```
app/src/main/java/com/geupsik/alimi/
├─ data/
│  ├─ remote/       나이스 API, 응답 파싱
│  ├─ local/        Room (일주일치 급식 캐시)
│  ├─ prefs/        DataStore (학교·알레르기·알림 설정)
│  ├─ repository/   화면이 쓰는 데이터 창구
│  └─ AppError.kt   오류를 사용자 안내 문구로 변환
├─ domain/          메뉴 파싱(알레르기 번호 분리), 알레르기 목록
├─ notification/    매일 알림 (알람 예약·수신·재부팅 복구)
├─ update/          깃허브 최신 릴리스 확인
└─ ui/
   ├─ onboarding/   학교 검색·등록
   ├─ meal/         급식 화면
   ├─ allergy/      알레르기 등록
   └─ settings/     설정
```
