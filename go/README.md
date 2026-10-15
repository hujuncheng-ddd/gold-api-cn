# gold-api (Go)

> GoldAPI (gold-api.cn) 贵金属行情 Go SDK —— 一次请求，批量拿回黄金 / 白银 / 铂金 / 钯金等 **70+ 品种**行情

## 安装

```bash
go get github.com/hujuncheng-ddd/gold-api-cn/go
```

零第三方依赖，Go 1.21+，标准库实现。

## 快速开始

```go
package main

import (
	"errors"
	"fmt"
	"log"

	goldapi "github.com/hujuncheng-ddd/gold-api-cn/go"
)

func main() {
	client := goldapi.NewClient("gk_your_appkey") // https://www.gold-api.cn/client/register.php 免费注册获取

	quotes, err := client.Realtime("1051", "1061", "hf_GC") // 黄金T+D + 黄金现货 + 纽约金
	if err != nil {
		var apiErr *goldapi.Error
		if errors.As(err, &apiErr) && apiErr.IsRateLimit() {
			log.Fatal("触发限流，退避后重试", apiErr.MsgID)
		}
		log.Fatal(err)
	}

	for _, q := range quotes {
		fmt.Printf("%s %v（%s）\n", q.VarietyName, q.LastPrice, q.ChangeMargin)
	}

	fmt.Printf("%+v\n", client.LastRateLimit()) // 配额感知
}
```

更多用法（单品种 `RealtimeOne`、品种列表 `Varieties`、自定义超时）见[仓库根目录 README](../README.md)。

## 链接

| 资源 | 地址 |
|---|---|
| 官网 | https://www.gold-api.cn |
| 接口文档 | https://www.gold-api.cn/docs.php |
| 免费注册 | https://www.gold-api.cn/client/register.php |

## License

MIT
