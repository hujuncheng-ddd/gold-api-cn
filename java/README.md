# GoldAPI Java SDK

> 贵金属实时行情 Java SDK —— 零第三方依赖，**Java 8+**，内置极简 JSON 解析器
> [官网](https://www.gold-api.cn) · [接口文档](https://www.gold-api.cn/docs.php) · [免费注册获取 AppKey](https://www.gold-api.cn/client/register.php)

## 安装（JitPack）

**第一步**：在 `pom.xml` 添加 JitPack 仓库（已用其他仓库可跳过）：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

**第二步**：添加依赖：

```xml
<dependency>
    <groupId>com.github.hujuncheng-ddd</groupId>
    <artifactId>gold-api-sdk</artifactId>
    <version>v1.3.0</version>
</dependency>
```

Gradle (Kotlin DSL)：

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.hujuncheng-ddd:gold-api-sdk:v1.3.0") }
```

## 快速开始

```java
import com.goldapi.sdk.GoldApiClient;
import com.goldapi.sdk.Quote;

GoldApiClient client = new GoldApiClient("gk_your_appkey");

// 批量查询（一次最多 50 个品种）
Map<String, Quote> quotes = client.realtime("1051", "1061", "hf_GC"); // 黄金T+D + 黄金现货 + 纽约金

for (Quote q : quotes.values()) {
    System.out.println(q.getVarietyName() + " 最新价 " + q.getLastPrice()
            + "（" + q.getChangeMargin() + "）");
}
```

## API 一览

| 方法 | 说明 |
|---|---|
| `realtime(String... goldIds)` | 批量实时行情（≤50 品种），返回以 goldId 为键的 `Map<String, Quote>` |
| `realtimeOne(String goldId)` | 单品种快捷查询，不存在时返回 `null` |
| `varieties()` / `varieties(String market)` | 品种列表（**无需 AppKey**），可按市场筛选如 `"上金所"`、`"外盘"` |
| `getLastRateLimit()` | 最近一次请求的配额 / 限流信息（来自响应头） |
| `getLastRequestId()` | 最近一次请求的 requestId（排查问题时提供给平台方） |

错误统一抛 `GoldApiException`（RuntimeException）：`getMsgId()` 平台错误码、`getRequestId()`、`getHttpCode()`、`isRateLimit()` 判断配额/频率类错误。

## 运行示例

```bash
export GOLD_API_KEY=gk_your_appkey   # https://www.gold-api.cn/client/register.php 免费注册获取

mvn package -pl java                 # 构建
java -cp java/target/classes:java/target/examples com.goldapi.examples.RealtimeExample   # 实时行情（11 字段）
java -cp java/target/classes:java/target/examples com.goldapi.examples.VarietiesExample  # 品种列表（无需 key）
```

## 安全提示

- AppKey 通过 `X-API-KEY` 请求头传递，不会出现在 URL / 访问日志中
- 切勿将 AppKey 写入前端或提交到公开仓库，建议通过环境变量注入

## License

MIT
