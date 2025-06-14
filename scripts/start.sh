#!/bin/bash

echo "✅ Jasypt 비밀번호를 Parameter Store에서 가져오는 중..."

JASYPT_ENCRYPTOR_PASSWORD=$(aws ssm get-parameter \
  --name "JASYPT_ENCRYPTOR_PASSWORD" \
  --with-decryption \
  --region ap-northeast-2 \
  --query "Parameter.Value" \
  --output text)

if [ -z "$JASYPT_ENCRYPTOR_PASSWORD" ]; then
  echo "❌ Jasypt 비밀번호를 가져오지 못했습니다. 실행을 중단합니다."
  exit 1
fi

echo "🔐 복호화 키 불러오기 완료"

echo "🚀 Spring Boot 애플리케이션 실행 중..."
nohup java -Djasypt.encryptor.password=$JASYPT_ENCRYPTOR_PASSWORD -jar /home/ec2-user/app/app.jar > /home/ec2-user/app/app.log 2>&1 &
