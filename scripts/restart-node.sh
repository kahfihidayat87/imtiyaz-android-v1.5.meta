#!/usr/bin/env bash
# Restart Node.js api.pastiumrah.com — battle-tested
# Pemakaian: bash scripts/restart-node.sh

set -e
SSH_PORT=65002
SSH_USER=u120369480
SSH_HOST=153.92.10.222

ssh -p "$SSH_PORT" "$SSH_USER@$SSH_HOST" 'bash -s' <<'REMOTE_EOF'
cd ~/domains/api.pastiumrah.com/hbuilds/current/nodejs
NODE=/opt/alt/alt-nodejs24/root/usr/bin/node

echo "=== 1. Kill proses lama ==="
PIDS=$(ps aux | grep 'api.pastiumrah' | grep -E 'lsnode|node app' | grep -v grep | awk '{print $2}')
if [ -n "$PIDS" ]; then
  for pid in $PIDS; do kill -9 "$pid" 2>/dev/null && echo "  KILL -9 -> $pid"; done
else
  echo "  Tidak ada proses aktif"
fi
sleep 3

echo ""
echo "=== 2. Pastikan port 3000 bebas ==="
REMAINING=$(ps aux | grep -E 'node app\.js' | grep -v grep | awk '{print $2}')
if [ -n "$REMAINING" ]; then
  echo "  Masih ada: $REMAINING"
  for pid in $REMAINING; do kill -9 "$pid" 2>/dev/null; done
  sleep 3
fi

echo ""
echo "=== 3. Arsip log lama ==="
[ -f console.log ] && mv console.log console.log.old-$(date +%s)
[ -f stderr.log ] && mv stderr.log stderr.log.old-$(date +%s)

echo ""
echo "=== 4. Spawn app baru ==="
nohup $NODE app.js > console.log 2> stderr.log < /dev/null &
NEW_PID=$!
echo "  Spawned PID: $NEW_PID"
sleep 8

echo ""
echo "=== 5. Verifikasi proses ==="
ps aux | grep "node app.js" | grep -v grep

echo ""
echo "=== 6. Log startup ==="
tail -10 console.log

echo ""
echo "=== 7. Log error ==="
tail -5 stderr.log

echo ""
echo "=== 8. Test localhost ==="
curl -s -o /dev/null -w "localhost:3000 -> HTTP %{http_code}\n" --max-time 5 http://localhost:3000/ 2>/dev/null || echo "curl gagal (normal)"

echo ""
echo "=== DONE ==="
REMOTE_EOF
