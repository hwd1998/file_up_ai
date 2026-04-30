#!/bin/bash
echo "===== 执行单元测试 ====="
mvn test
if [ $? -eq 0 ]; then
  echo "✅ 测试通过"
  exit 0
else
  echo "❌ 测试不通过"
  exit 1
fi