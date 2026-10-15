#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""演示：批量获取实时行情。

运行前:
  1. pip install gold-api   (或 pip install -e python/)
  2. 免费注册获取 AppKey: https://www.gold-api.cn/client/register.php
  3. export GOLD_API_KEY=gk_xxxxxxxx  (或直接替换下方占位符)

运行: python realtime.py
"""
import os

from gold_api import GoldApiClient, GoldApiException

appkey = os.environ.get("GOLD_API_KEY") or "gk_你的appkey"

client = GoldApiClient(appkey)

try:
    # 一次请求拿 4 个品种：黄金T+D / 黄金现货 / 白银现货 / 纽约金
    quotes = client.realtime(["1051", "1061", "1062", "hf_GC"])

    if not quotes:
        raise SystemExit("未查询到行情，请检查 goldId 是否正确")

    # 每个品种返回的完整字段（11 个）：
    #   lastPrice 最新价 / changePrice 涨跌额 / changeMargin 涨跌幅
    #   yesyPrice 昨收 / openPrice 开盘 / highPrice 最高 / lowPrice 最低
    #   buyPrice 买入价 / sellPrice 卖出价 / updateTime 更新时间
    fmt = "%-12s %10s %9s %9s %10s %10s %10s %10s %10s %10s  %s"
    print(fmt % ("品种", "最新价", "涨跌额", "涨跌幅", "昨收", "开盘", "最高", "最低", "买入价", "卖出价", "更新时间"))
    print("-" * 122)
    for q in quotes.values():
        print(fmt % (
            q["varietyName"], q["lastPrice"], q["changePrice"], q["changeMargin"],
            q["yesyPrice"], q["openPrice"], q["highPrice"], q["lowPrice"],
            q["buyPrice"], q["sellPrice"], q["updateTime"]))

    # 配额 / 限流信息（来自响应头，可用于客户端自行控制节奏）
    print("\n配额信息:", client.last_rate_limit())
except GoldApiException as e:
    print("调用失败 [%s] HTTP %d: %s (requestId: %s)" % (
        e.msg_id or "-", e.http_code, e, e.request_id or "-"))
    if e.is_rate_limit():
        print("提示: 已触发配额/频率限制，可降低调用频率或提升配额")
    raise SystemExit(1)
