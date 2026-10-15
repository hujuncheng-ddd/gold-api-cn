# GoldAPI PHP SDK

> 🥇 贵金属实时行情 PHP SDK —— 一次请求，批量拿回黄金 / 白银 / 铂金 / 钯金等 **70+ 品种**行情
> 数据服务由 [gold-api.cn](https://www.gold-api.cn) 提供 · [免费注册即得 AppKey](https://www.gold-api.cn/client/register.php)

![PHP](https://img.shields.io/badge/PHP-%3E%3D7.2-8892BF?logo=php&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)
![API](https://img.shields.io/badge/API-v1-blue)
![No Dependencies](https://img.shields.io/badge/dependencies-zero-success)

## ✨ 为什么选 GoldAPI

| 特性 | 说明 |
|---|---|
| 🪙 **70+ 品种** | 上金所 T+D、现货、机构报价、纽约外盘、沪金主力、广铂/广钯、汇率、美元指数全覆盖 |
| 📦 **批量查询** | 一次请求最多 **50 个品种**，字段结构统一，无需逐个轮询 |
| 🎫 **密钥分发** | 一个账户多把 AppKey，各自限定品种范围，随时停用/重置 |
| 📊 **精确计量** | 调用明细随时可查，响应头实时返回配额余量 |
| 🧾 **字段稳定** | 返回结构长期稳定，接入方无需改动解析逻辑 |
| 🚀 **即插即用** | 纯 cURL 实现，**零第三方依赖**，PHP 7.2+ 开箱即用 |

## 🚀 快速开始（3 步）

**1. 免费注册获取 AppKey** 👉 [https://www.gold-api.cn/client/register.php](https://www.gold-api.cn/client/register.php)

**2. 安装**

```bash
composer require gold-api/sdk
```

**3. 一行代码拿行情**

```php
use GoldApi\Sdk\GoldApiClient;

$client = new GoldApiClient('gk_your_appkey');

$quotes = $client->realtime(['1061', 'hf_GC', 'nf_AU0']);  // 黄金现货 + 纽约金 + 沪金主力

foreach ($quotes as $goldId => $q) {
    echo $q['varietyName'], ' 最新价 ', $q['lastPrice'], '（', $q['changeMargin'], '）', PHP_EOL;
}
```

输出：

```
黄金现货 最新价 897.6（-3.33%）
纽约金 最新价 2651.2（+0.42%）
沪金主力 最新价 618.35（-2.91%）
```

## 📖 更多用法

### 单品种快捷查询

```php
$quote = $client->realtimeOne('1061');          // 黄金现货
// 品种不存在 / 已下架时返回 null，注意判空

$price = $quote['lastPrice'];                   // 最新价
$change = $quote['changeMargin'];               // 涨跌幅 "-3.33%"
```

### 品种列表（无需 AppKey，可直接给前端下拉框用）

```php
$list = $client->varieties();                   // 全部品种
$shList = $client->varieties('上金所');          // 按市场筛选
```

### 配额 / 限流感知

```php
$quotes = $client->realtime('1061');

$client->getLastRateLimit();
// ['billing' => 'member', 'daily_limit' => 1000, 'daily_remaining' => 973, 'qps_limit' => 5]

$client->getLastRequestId();                    // 排查问题时提供给平台方
```

### 完整错误处理

```php
use GoldApi\Sdk\Exception\GoldApiException;

try {
    $quotes = $client->realtime(['1061', 'hf_GC']);
} catch (GoldApiException $e) {
    echo '错误码: ', $e->getMsgId(), PHP_EOL;       // 如 DAILY_LIMIT_EXCEEDED
    echo '信息: ', $e->getMessage(), PHP_EOL;
    echo 'requestId: ', $e->getRequestId(), PHP_EOL; // 报障时提供

    if ($e->isRateLimit()) {
        // 配额/频率类错误：退避重试或提醒升级配额
        sleep(1);
    }
}
```

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
- 本 SDK 默认通过 `X-API-KEY` 请求头传密钥，避免 appkey 泄漏到 URL / 访问日志
- 密钥泄露后请在管理后台「密钥管理」中立即重置，旧密钥即时失效
- 建议通过环境变量注入：`new GoldApiClient(getenv('GOLD_API_KEY'))`

## 🔗 相关链接

| 资源 | 地址 |
|---|---|
| 🏠 官网 | https://www.gold-api.cn |
| 📝 接口文档 | https://www.gold-api.cn/docs.php |
| 🪙 品种对照表 | https://www.gold-api.cn/varieties.php |
| 🎫 免费注册 | https://www.gold-api.cn/client/register.php |
| 💬 问题反馈 | [Issues](https://github.com/hujuncheng-ddd/gold-api-cn/issues) |

## License

[MIT](LICENSE)

---

<p align="center">
  🥇 <strong>金价数据，一条请求全拿到</strong><br>
  <a href="https://www.gold-api.cn/client/register.php">免费注册，立即使用 →</a>
</p>
