<?php
/**
 * 演示：批量获取实时行情
 *
 * 运行前：
 *   1. composer install
 *   2. 免费注册获取 AppKey: https://www.gold-api.cn/client/register.php
 *   3. export GOLD_API_KEY=gk_xxxxxxxx （或直接替换下方占位符）
 *
 * 运行: php examples/realtime.php
 */

require __DIR__ . '/../vendor/autoload.php';

use GoldApi\Sdk\Exception\GoldApiException;
use GoldApi\Sdk\GoldApiClient;

$appkey = getenv('GOLD_API_KEY') ?: 'gk_你的appkey';

$client = new GoldApiClient($appkey);

try {
    // 一次请求拿 4 个品种：黄金现货 / 白银现货 / 纽约金 / 沪金主力
    // 全部 goldId 见 https://www.gold-api.cn/varieties.php
    $quotes = $client->realtime(['1061', '1062', 'hf_GC', 'nf_AU0']);

    if (!$quotes) {
        exit("未查询到行情，请检查 goldId 是否正确\n");
    }

    printf("%-14s %12s %10s %10s  %s\n", '品种', '最新价', '涨跌额', '涨跌幅', '更新时间');
    echo str_repeat('-', 66), "\n";
    foreach ($quotes as $q) {
        printf(
            "%-14s %12.2f %10.2f %10s  %s\n",
            $q['varietyName'],
            $q['lastPrice'],
            $q['changePrice'],
            $q['changeMargin'],
            $q['updateTime']
        );
    }

    // 配额 / 限流信息（来自响应头，可用于客户端自行控制节奏）
    echo "\n配额信息: ";
    print_r($client->getLastRateLimit());
} catch (GoldApiException $e) {
    fwrite(STDERR, sprintf(
        "调用失败 [%s] HTTP %d: %s (requestId: %s)\n",
        $e->getMsgId() ?: '-',
        $e->getHttpCode(),
        $e->getMessage(),
        $e->getRequestId() ?: '-'
    ));
    if ($e->isRateLimit()) {
        fwrite(STDERR, "提示: 已触发配额/频率限制，可降低调用频率或提升配额\n");
    }
    exit(1);
}
