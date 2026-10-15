#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""演示：获取品种列表（无需 AppKey，可直接用于前端下拉框）。

运行: python varieties.py [市场名称]
示例: python varieties.py 上金所
"""
import sys

from gold_api import GoldApiClient

market = sys.argv[1] if len(sys.argv) > 1 else None

client = GoldApiClient()  # varieties 接口无需认证

items = client.varieties(market)

if not items:
    raise SystemExit("未查询到品种")

print("%-10s %-14s %-10s %s" % ("goldId", "名称", "市场", "分类"))
print("-" * 48)
for v in items:
    print("%-10s %-14s %-10s %s" % (v["goldId"], v["name"], v["market"], v["category"]))
print("\n共 %d 个品种" % len(items))
