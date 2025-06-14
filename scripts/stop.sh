# scripts/stop.sh
#!/bin/bash
echo "🛑 기존 프로세스 종료 중..."
pkill -f 'java.*app.jar' || true
