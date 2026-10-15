<?php
/**
 * 演示：获取品种列表（无需 AppKey，可直接用于前端下拉框）
 *
 * 运行: php examples/varieties.php [市场名称]
 * 示例: php examples/varieties.php 上金所
 */

require __DIR__ . '/../vendor/autoload.php';

use GoldApi\Sdk\GoldApiClient;

$market = isset($argv[1]) ? $argv[1] : null;

$client = new GoldApiClient(''); // varieties 接口无需认证，appkey 传空即可

$list = $client->varieties($market);

if (!$list) {
    exit("未查询到品种\n");
}

printf("%-10s %-14s %-10s %s\n", 'goldId', '名称', '市场', '分类');
echo str_repeat('-', 48), "\n";
foreach ($list as $v) {
    printf("%-10s %-14s %-10s %s\n", $v['goldId'], $v['name'], $v['market'], $v['category']);
}
echo "\n共 ", count($list), " 个品种\n";
