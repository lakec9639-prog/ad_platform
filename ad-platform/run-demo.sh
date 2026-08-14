#!/bin/bash
# LUMI ADX Phase 1 Demo Script
# Starts services and runs simulator

set -e

echo "=== LUMI ADX Phase 1 Demo ==="

# 1. Start infrastructure
echo "Starting MySQL + Redis..."
cd "$(dirname "$0")"
docker compose up -d mysql redis
echo "Waiting for MySQL..."
sleep 10

# 2. Initialize DB (already done via docker-entrypoint-initdb.d)

# 3. Start Management Service
echo "Starting Management Service..."
cd backend
mvn spring-boot:run -q &
MANAGEMENT_PID=$!
cd ..

# 4. Start Bidding Service
echo "Starting Bidding Service..."
cd bidding-service
mvn compile exec:java -Dexec.mainClass="com.ad.bidding.BiddingApplication" -q &
BIDDING_PID=$!
cd ..

echo "Waiting for services to start..."
sleep 15

# 5. Create a test publisher and ad slot via Management API
echo "Creating test publisher..."
curl -s -X POST http://localhost:8080/api/v1/publishers \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Media","code":"DEMO001","revenueShare":0.7}'

echo ""
echo "Creating test ad slot..."
curl -s -X POST http://localhost:8080/api/v1/ad-slots \
  -H "Content-Type: application/json" \
  -d '{"publisherId":1,"name":"Banner 320x480","code":"SLOT_001","slotType":1,"width":320,"height":480,"floorPrice":0.01}'

echo ""
echo "=== Services Ready ==="
echo "  Management: http://localhost:8080"
echo "  Bidding:    http://localhost:9090"
echo ""

# 6. Run simulator
echo "Running simulator (100 requests)..."
cd bidding-service
mvn compile exec:java -Dexec.mainClass="com.ad.bidding.simulator.MediaSimulator" -Dexec.args="100 10" -q
cd ..

# 7. Cleanup
echo "Stopping services..."
kill $MANAGEMENT_PID $BIDDING_PID 2>/dev/null
echo "Demo complete."
