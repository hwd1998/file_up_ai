#!/bin/bash
echo "===== 代码格式校验 ====="
mvn com.alibaba.p3c:p3c-maven-plugin:check
if [ $? -eq 0 ]; then
  echo "✅ 格式校验通过"
  exit 0
else
  echo "❌ 格式校验失败"
  exit 1
fi