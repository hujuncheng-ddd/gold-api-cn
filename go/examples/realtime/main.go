// 演示：批量获取实时行情
//
// 运行前:
//  1. go get github.com/hujuncheng-ddd/gold-api-cn/go
//  2. 免费注册获取 AppKey: https://www.gold-api.cn/client/register.php
//  3. export GOLD_API_KEY=gk_xxxxxxxx（或直接替换下方占位符）
//
// 运行: go run ./examples/realtime
package main

import (
	"errors"
	"fmt"
	"log"
	"os"
	"strings"

	goldapi "github.com/hujuncheng-ddd/gold-api-cn/go"
)

func main() {
	appkey := os.Getenv("GOLD_API_KEY")
	if appkey == "" {
		appkey = "gk_你的appkey" // https://www.gold-api.cn/client/register.php 免费注册获取
	}

	client := goldapi.NewClient(appkey)

	// 一次请求拿 4 个品种：黄金T+D / 黄金现货 / 白银现货 / 纽约金
	// 全部 goldId 见 https://www.gold-api.cn/varieties.php
	quotes, err := client.Realtime("1051", "1061", "1062", "hf_GC")
	if err != nil {
		var apiErr *goldapi.Error
		if errors.As(err, &apiErr) && apiErr.IsRateLimit() {
			log.Fatalf("调用失败 [%s] HTTP %d: %s（已触发配额/频率限制，可降低调用频率或提升配额）requestId: %s",
				apiErr.MsgID, apiErr.HTTPCode, apiErr.Msg, apiErr.RequestID)
		}
		log.Fatalf("调用失败: %v", err)
	}
	if len(quotes) == 0 {
		log.Fatal("未查询到行情，请检查 goldId 是否正确")
	}

	// 每个品种返回的完整字段（11 个）：
	//   LastPrice 最新价 / ChangePrice 涨跌额 / ChangeMargin 涨跌幅
	//   YesyPrice 昨收 / OpenPrice 开盘 / HighPrice 最高 / LowPrice 最低
	//   BuyPrice 买入价 / SellPrice 卖出价 / UpdateTime 更新时间
	fmt.Printf("%-12s %10s %9s %9s %10s %10s %10s %10s %10s %10s  %s\n",
		"品种", "最新价", "涨跌额", "涨跌幅", "昨收", "开盘", "最高", "最低", "买入价", "卖出价", "更新时间")
	fmt.Println(strings.Repeat("-", 122))
	for _, q := range quotes {
		fmt.Printf("%-12s %10.2f %9.2f %9s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f  %s\n",
			q.VarietyName, q.LastPrice, q.ChangePrice, q.ChangeMargin,
			q.YesyPrice, q.OpenPrice, q.HighPrice, q.LowPrice,
			q.BuyPrice, q.SellPrice, q.UpdateTime)
	}

	// 配额 / 限流信息（来自响应头，可用于客户端自行控制节奏）
	fmt.Printf("\n配额信息: %+v\n", client.LastRateLimit())
}
