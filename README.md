# 나라 걱정 클럽

## 빌드 방법
* ubuntu 접속 -> cd /home/ubuntu/nara_web
* ./start.sh

or
* bootJar 
* nohup java -jar naraclub-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
* 버전은 변경하셔서 적용하시면 됩니다.

## 프로젝트 정보 
- JAVA : 21 
- Gradle
- JPA / QueryDSL
- spring boot : 3.3.11-SNAPSHOT

## 서버 정보

### nginx  
- nginx -V
- nginx version: nginx/1.14.0 (Ubuntu)
- 공인 IP : 1.201.174.213
- 내부 IP : 192.168.10.7
- ssh 계정 : ubuntu / Ministry2025!@# 

 * web도 /data 공간이 400G 있는데, DB처럼 따로 경로를 잡지는않았어요. 
 
### 스프링
 - ubuntu 계정 : /home/ubuntu/nara_web   => 여기서 jar 파일을 넣으면 됨   
 - 포트 : 80/443(nginx)  -> 8032(spring) 

 * 한글 도메인이라 소스에서 호출시 :   www.xn--w69at2fhshwrs.kr 또는  xn--w69at2fhshwrs.kr
 * www.나라걱정.kr 입력시 -> www.club1.newstomato.com 으로 리다이렉션 됩니다. 
 * Twitter / kakao CallBack Url은 www.club1.newstomato.com 으로 세팅 하셔야 합니다.

### 자바
- java --version
- openjdk 21.0.2 2024-01-16 LTS
- OpenJDK Runtime Environment Temurin-21.0.2+13 (build 21.0.2+13-LTS)
- OpenJDK 64-Bit Server VM Temurin-21.0.2+13 (build 21.0.2+13-LTS, mixed mode, sharing)

### DB 
* MariaDB 10.1.48 database server
* DB계정 : root / tomato0425@!
* 공인 IP : 1.201.174.218
* 내부 IP : 192.168.10.12
* ssh 계정 : ubuntu / Ministry2025!@# 
* DB 경로 : 기존(/var/lib/mysql)  -> 변경(/data) : 800G  


## 외부 API 접속 정보 

### x (구 트위터)
* url : https://developer.x.com/en 
* 아이디 : tomatochain_official@etomato.com
* 비번 : GKQWJDEHD789!@!

### 카카오 
* url : https://developers.kakao.com/
* 아이디 : tomatochain@etomato.com
* 비번 : ttchain1!

### 나이스(PASS)
* 사이트 : https://www.niceapi.co.kr/
* 아이디 : tomatochain
* 비번 : tongtong77!!

* Key 조회 루트 : 오른쪽 상단 사람 아이콘 > My APP List
* 가이드 루트 : Products > 본인확인(통합형) 개발가이드
