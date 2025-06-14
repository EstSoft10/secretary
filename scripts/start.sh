# scripts/start.sh
#!/bin/bash
echo "✅ Spring Boot 시작 중..."
nohup java -jar /home/ec2-user/app.jar > /dev/null 2>&1 &
