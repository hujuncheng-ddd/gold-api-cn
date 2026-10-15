# GoldAPI Official SDK

> 🥇 贵金属实时行情官方 SDK —— 一次请求，批量拿回黄金 / 白银 / 铂金 / 钯金等 **70+ 品种**行情
> 数据服务由 [gold-api.cn](https://www.gold-api.cn) 提供 · [免费注册即得 AppKey](https://www.gold-api.cn/client/register.php)

![PHP](https://img.shields.io/badge/PHP-%3E%3D7.2-8892BF?logo=php&logoColor=white)
![Python](https://img.shields.io/badge/Python-%3E%3D3.7-3776AB?logo=python&logoColor=white)
![Node](https://img.shields.io/badge/Node.js-%3E%3D18-339933?logo=nodedotjs&logoColor=white)
![Go](https://img.shields.io/badge/Go-%3E%3D1.21-00ADD8?logo=go&logoColor=white)
![Java](https://img.shields.io/badge/Java-%3E%3D8-007396?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)
![No Dependencies](https://img.shields.io/badge/dependencies-zero-success)

## 📦 选择你的语言

| 语言 | 安装 | 目录 | 依赖要求 |
|---|---|---|---|
| **PHP** | `composer require gold-api/sdk-php` | 根目录 | PHP ≥ 7.2，ext-curl |
| **Python** | `pip install gold-api` | [`python/`](python/) | Python ≥ 3.7，零依赖 |
| **Node.js** | `npm install gold-api` | [`node/`](node/) | Node ≥ 18，零依赖（内置 fetch） |
| **Go** | `go get github.com/hujuncheng-ddd/gold-api-cn/go` | [`go/`](go/) | Go ≥ 1.21，零依赖（标准库） |
| **Java** | JitPack `com.github.hujuncheng-ddd:gold-api-sdk` | [`java/`](java/) | Java ≥ 8，零依赖（含内置 JSON 解析） |

## ✨ 为什么选 GoldAPI

| 特性 | 说明 |
|---|---|
| 🪙 **70+ 品种** | 上金所 T+D、现货、机构报价、纽约外盘、沪金主力、广铂/广钯、汇率、美元指数全覆盖 |
| 📦 **批量查询** | 一次请求最多 **50 个品种**，字段结构统一，无需逐个轮询 |
| 🎫 **密钥分发** | 一个账户多把 AppKey，各自限定品种范围，随时停用/重置 |
| 📊 **精确计量** | 调用明细随时可查，响应头实时返回配额余量 |
| 🧾 **字段稳定** | 返回结构长期稳定，接入方无需改动解析逻辑 |
| 🚀 **即插即用** | 五个 SDK 全部**零第三方依赖**，开箱即用 |

## 🚀 快速开始（3 步）

**1. 免费注册获取 AppKey** 👉 [https://www.gold-api.cn/client/register.php](https://www.gold-api.cn/client/register.php)

**2. 安装**（任选其一）

```bash
composer require gold-api/sdk-php   # PHP
pip install gold-api            # Python
npm install gold-api            # Node.js
go get github.com/hujuncheng-ddd/gold-api-cn/go   # Go
# Java：JitPack 依赖 com.github.hujuncheng-ddd:gold-api-sdk（见下方 Java 标签页）
```

**3. 一行代码拿行情**

<details open>
<summary><b>PHP</b></summary>

```php
use GoldApi\Sdk\GoldApiClient;

$client = new GoldApiClient('gk_your_appkey');

$quotes = $client->realtime(['1051', '1061', 'hf_GC']);  // 黄金T+D + 黄金现货 + 纽约金

foreach ($quotes as $goldId => $q) {
    echo $q['varietyName'], ' 最新价 ', $q['lastPrice'], '（', $q['changeMargin'], '）', PHP_EOL;
}
```
</details>

<details>
<summary><b>Python</b></summary>

```python
from gold_api import GoldApiClient

client = GoldApiClient("gk_your_appkey")

quotes = client.realtime(["1051", "1061", "hf_GC"])  # 黄金T+D + 黄金现货 + 纽约金

for q in quotes.values():
    print(q["varietyName"], "最新价", q["lastPrice"], f'（{q["changeMargin"]}）')
```
</details>

<details>
<summary><b>Node.js</b></summary>

```js
import { GoldApiClient } from 'gold-api';

const client = new GoldApiClient('gk_your_appkey');

const quotes = await client.realtime(['1051', '1061', 'hf_GC']); // 黄金T+D + 黄金现货 + 纽约金

for (const q of Object.values(quotes)) {
  console.log(`${q.varietyName} 最新价 ${q.lastPrice}（${q.changeMargin}）`);
}
```
</details>

<details>
<summary><b>Go</b></summary>

```go
client := goldapi.NewClient("gk_your_appkey")

quotes, _ := client.Realtime("1051", "1061", "hf_GC") // 黄金T+D + 黄金现货 + 纽约金

for _, q := range quotes {
    fmt.Println(q.VarietyName, "最新价", q.LastPrice, "（"+q.ChangeMargin+"）")
}
```
</details>

<details>
<summary><b>Java</b></summary>

```xml
<!-- pom.xml: 先添加 JitPack 仓库，再添加依赖 -->
<repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>

<dependency>
    <groupId>com.github.hujuncheng-ddd</groupId>
    <artifactId>gold-api-sdk</artifactId>
    <version>v1.3.0</version>
</dependency>
```

```java
GoldApiClient client = new GoldApiClient("gk_your_appkey");

Map<String, Quote> quotes = client.realtime("1051", "1061", "hf_GC"); // 黄金T+D + 黄金现货 + 纽约金

for (Quote q : quotes.values()) {
    System.out.println(q.getVarietyName() + " 最新价 " + q.getLastPrice() + "（" + q.getChangeMargin() + "）");
}
```
</details>

五个 SDK 的 API 完全对齐：

| 能力 | PHP | Python | Node.js | Go | Java |
|---|---|---|---|---|---|
| 批量实时行情（≤50 品种） | `realtime()` | `realtime()` | `realtime()` | `Realtime()` | `realtime()` |
| 单品种快捷查询 | `realtimeOne()` | `realtime_one()` | `realtimeOne()` | `RealtimeOne()` | `realtimeOne()` |
| 品种列表（免认证） | `varieties()` | `varieties()` | `varieties()` | `Varieties()` | `varieties()` |
| 配额/限流感知 | `getLastRateLimit()` | `last_rate_limit()` | `getLastRateLimit()` | `LastRateLimit()` | `getLastRateLimit()` |
| 异常（msgId/requestId/httpCode + 限流判断） | `GoldApiException` | `GoldApiException` | `GoldApiException` | `*goldapi.Error` | `GoldApiException` |

## 📊 返回字段（每个品种 11 个字段）

```json
{
  "goldId": "hf_GC",
  "variety": "COMEX黄金",
  "varietyName": "纽约金",
  "lastPrice": 4159.012,
  "buyPrice": 4156.8,
  "sellPrice": 4157.0,
  "highPrice": 4183.3,
  "lowPrice": 4120.9,
  "openPrice": 4128.3,
  "yesyPrice": 4107.9,
  "changePrice": 51.112,
  "changeMargin": "1.24%",
  "updateTime": "2026-10-15 14:35:22"
}
```

| 字段 | 说明 |
|---|---|
| `lastPrice` | 最新价 |
| `changePrice` / `changeMargin` | 涨跌额 / 涨跌幅（`-3.33%`） |
| `yesyPrice` | 昨收价（涨跌基准价） |
| `openPrice` | 开盘价（现货类品种可能为 `0`） |
| `highPrice` / `lowPrice` | 当日最高 / 最低 |
| `buyPrice` / `sellPrice` | 买入价 / 卖出价 |
| `updateTime` | 行情更新时间（北京时间） |

## 🪙 常用 goldId 速查

| goldId | 品种 | 市场 |
|---|---|---|
| `1061` / `1062` | 黄金现货 / 白银现货 | 现货 |
| `1051` / `1052` | 黄金T+D / 白银T+D | 上金所 |
| `1053` | 黄金9999 | 上金所 |
| `hf_GC` / `hf_SI` | 纽约金 / 纽约银 | 外盘 |
| `hf_XAU` / `hf_XAG` | XAUUSD / XAGUSD | 外盘 |
| `nf_AU0` / `nf_AG0` | 沪金主力 / 沪银主力 | 上期所 |
| `gf_PT0` / `gf_PD0` | 广铂主力 / 广钯主力 | 广期所 |
| `hk_gold` | 港金 | 香港 |
| `DXY` | 美元指数 | 指数 |
| `USDCNY` | 美元兑人民币 | 汇率 |

👉 [全部 70 个品种明细](https://www.gold-api.cn/varieties.php)

## ⚠️ 安全提示

- AppKey 等同于账号密码，**切勿写入前端 JS 或提交到公开仓库**
- 五个 SDK 均默认通过 `X-API-KEY` 请求头传密钥，避免 appkey 泄漏到 URL / 访问日志
- 密钥泄露后请在管理后台「密钥管理」中立即重置，旧密钥即时失效
- 建议通过环境变量注入（如 `GOLD_API_KEY`）

## 🔗 相关链接

| 资源 | 地址 |
|---|---|
| 🏠 官网 | https://www.gold-api.cn |
| 📝 接口文档 | https://www.gold-api.cn/docs.php |
| 🪙 品种对照表 | https://www.gold-api.cn/varieties.php |
| 🎫 免费注册 | https://www.gold-api.cn/client/register.php |
| 📦 PHP SDK | [仓库根目录](src/GoldApiClient.php) · Packagist `gold-api/sdk-php` |
| 🐍 Python SDK | [`python/`](python/) · PyPI `gold-api` |
| 🟢 Node SDK | [`node/`](node/) · npm `gold-api` |
| ☕ Java SDK | [`java/`](java/) · JitPack `com.github.hujuncheng-ddd:gold-api-sdk` |
| 💬 问题反馈 | [Issues](https://github.com/hujuncheng-ddd/gold-api-cn/issues) |

## License

[MIT](LICENSE)

---

<p align="center">
  🥇 <strong>金价数据，一条请求全拿到</strong><br>
  <a href="https://www.gold-api.cn/client/register.php">免费注册，立即使用 →</a>
</p>
