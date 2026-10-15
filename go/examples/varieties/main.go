// 演示：品种列表（无需 AppKey）
//
// 运行: go run ./examples/varieties
package main

import (
	"fmt"
	"log"

	goldapi "github.com/hujuncheng-ddd/gold-api-cn/go"
)

func main() {
	client := goldapi.NewClient("") // varieties 接口无需认证

	items, err := client.Varieties("上金所")
	if err != nil {
		log.Fatalf("调用失败: %v", err)
	}

	fmt.Printf("上金所共 %d 个品种:\n", len(items))
	for _, v := range items {
		fmt.Printf("  %-8s %-10s %s / %s\n", v.GoldID, v.Name, v.Market, v.Category)
	}
}
