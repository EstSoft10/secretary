#!/bin/bash
echo "✅ Spring Boot 시작 중..."
nohup java -Djasypt.encryptor.password=$JASYPT_ENCRYPTOR_PASSWORD -jar /home/ec2-user/app/app.jar > /home/ec2-user/app/app.log 2>&1 &
