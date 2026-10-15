<?php
/**
 * GoldAPI 数据服务平台 PHP SDK
 *
 * 官网:   https://www.gold-api.cn
 * 文档:   https://www.gold-api.cn/docs.php
 * 免费注册: https://www.gold-api.cn/client/register.php
 */

declare(strict_types=1);

namespace GoldApi\Sdk;

use GoldApi\Sdk\Exception\GoldApiException;

/**
 * GoldAPI 客户端
 *
 * 用法:
 *   $client = new GoldApiClient('gk_your_appkey');
 *   $quotes = $client->realtime(['1061', 'hf_GC']);   // 黄金现货 + 纽约金
 */
class GoldApiClient
{
    /** 接口根地址 */
    const DEFAULT_BASE_URL = 'https://www.gold-api.cn';

    /** 单次请求最多品种数（平台限制） */
    const MAX_IDS_PER_REQUEST = 50;

    /** @var string */
    private $appkey;

    /** @var string */
    private $baseUrl;

    /** @var int 整体请求超时（秒） */
    private $timeout = 5;

    /** @var int 连接超时（秒） */
    private $connectTimeout = 2;

    /** @var array 最近一次响应头（键名已转小写） */
    private $lastHeaders = [];

    /**
     * @param string $appkey  平台签发的 AppKey（https://www.gold-api.cn/client/register.php 免费注册获取）；
     *                        仅调用 varieties() 等公开接口时可传空字符串
     * @param string $baseUrl 接口根地址，默认线上地址
     */
    public function __construct(string $appkey, string $baseUrl = self::DEFAULT_BASE_URL)
    {
        $this->appkey = $appkey;
        $this->baseUrl = rtrim($baseUrl, '/');
    }

    /* =====================================================================
     *  公开 API
     * ===================================================================== */

    /**
     * 实时行情（支持批量，单次最多 50 个品种）
     *
     * @param string|array $goldIds '1061,1062' 或 ['1061', '1062', 'hf_GC']
     *
     * @return array<string, array> 以 goldId 为键的行情数组；goldId 不存在时该键缺失
     *
     * @throws GoldApiException    认证失败 / 配额超限 / 网络错误等
     * @throws \InvalidArgumentException 参数为空或超过 50 个品种
     */
    public function realtime($goldIds): array
    {
        if (\is_array($goldIds)) {
            $goldIds = implode(',', $goldIds);
        }
        $goldIds = trim($goldIds);
        if ($goldIds === '') {
            throw new \InvalidArgumentException('goldIds 不能为空');
        }

        $count = substr_count($goldIds, ',') + 1;
        if ($count > self::MAX_IDS_PER_REQUEST) {
            throw new \InvalidArgumentException(sprintf(
                '单次最多 %d 个品种，当前 %d 个，请分批请求',
                self::MAX_IDS_PER_REQUEST,
                $count
            ));
        }

        $result = $this->request('/api/v1/gold/realtime', ['goldid' => $goldIds], true);

        return isset($result['dtList']) && \is_array($result['dtList']) ? $result['dtList'] : [];
    }

    /**
     * 单品种行情快捷方法
     *
     * @return array|null goldId 不存在或已下架时返回 null
     */
    public function realtimeOne(string $goldId): ?array
    {
        $list = $this->realtime($goldId);

        return $list[$goldId] ?? null;
    }

    /**
     * 品种列表（无需认证，可直接用于前端下拉框）
     *
     * @param string|null $market 按市场筛选，如 '上金所'、'外盘'、'现货'；null 为全部
     *
     * @return array 例如 [ ['goldId' => '1061', 'name' => '黄金现货', 'market' => '现货', 'category' => '黄金'], ... ]
     */
    public function varieties(?string $market = null): array
    {
        $query = [];
        if ($market !== null && $market !== '') {
            $query['market'] = $market;
        }

        $result = $this->request('/api/v1/gold/varieties', $query, false);

        return isset($result['dtList']) && \is_array($result['dtList']) ? $result['dtList'] : [];
    }

    /**
     * 最近一次请求的配额 / 限流信息（来自响应头，会员通道）
     *
     * @return array 如 ['billing' => 'member', 'daily_limit' => 1000, 'daily_remaining' => 973, 'qps_limit' => 5]
     */
    public function getLastRateLimit(): array
    {
        $info = [];
        if (isset($this->lastHeaders['x-billing'])) {
            $info['billing'] = $this->lastHeaders['x-billing'];
        }
        foreach ([
            'x-ratelimit-daily-limit'     => 'daily_limit',
            'x-ratelimit-daily-remaining' => 'daily_remaining',
            'x-ratelimit-qps-limit'       => 'qps_limit',
            'x-ratelimit-variety-qps'     => 'variety_qps',
        ] as $raw => $key) {
            if (isset($this->lastHeaders[$raw])) {
                $info[$key] = $this->lastHeaders[$raw];
            }
        }

        return $info;
    }

    /**
     * 最近一次请求的 requestId（联系平台排查问题时请提供该值）
     */
    public function getLastRequestId(): string
    {
        return $this->lastHeaders['x-request-id'] ?? '';
    }

    /** 设置整体请求超时（秒），默认 5 */
    public function setTimeout(int $seconds): self
    {
        $this->timeout = $seconds;

        return $this;
    }

    /** 设置连接超时（秒），默认 2 */
    public function setConnectTimeout(int $seconds): self
    {
        $this->connectTimeout = $seconds;

        return $this;
    }

    /* =====================================================================
     *  内部实现
     * ===================================================================== */

    /**
     * 发起 HTTP 请求并解析响应
     *
     * @return array 成功响应中的 result 对象
     */
    private function request(string $path, array $query, bool $withAuth): array
    {
        $url = $this->baseUrl . $path;
        if ($query) {
            $url .= '?' . http_build_query($query);
        }

        $headers = ['Accept: application/json'];
        if ($withAuth) {
            if ($this->appkey === '') {
                throw new \InvalidArgumentException('appkey 不能为空，请传入平台签发的 AppKey');
            }
            // 走请求头认证，避免 appkey 出现在 URL / 访问日志 / 浏览器历史中
            $headers[] = 'X-API-KEY: ' . $this->appkey;
        }

        $rawHeaders = '';
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT        => $this->timeout,
            CURLOPT_CONNECTTIMEOUT => $this->connectTimeout,
            CURLOPT_HTTPHEADER     => $headers,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_HEADERFUNCTION => function ($ch, $line) use (&$rawHeaders) {
                $rawHeaders .= $line;

                return \strlen($line);
            },
        ]);

        $raw = curl_exec($ch);

        if ($raw === false) {
            $err   = curl_error($ch);
            $errno = curl_errno($ch);
            curl_close($ch);
            throw new GoldApiException('网络请求失败: ' . $err, 0, $errno);
        }

        $httpCode = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        $this->lastHeaders = $this->parseHeaders($rawHeaders);

        $data = json_decode((string) $raw, true);
        if (!\is_array($data)) {
            throw new GoldApiException('响应不是合法 JSON: ' . substr((string) $raw, 0, 200), $httpCode);
        }

        if (($data['success'] ?? '0') !== '1') {
            throw GoldApiException::fromResponse($data, $httpCode);
        }

        return isset($data['result']) && \is_array($data['result']) ? $data['result'] : [];
    }

    /**
     * 解析原始响应头为小写键名数组
     */
    private function parseHeaders(string $raw): array
    {
        $out = [];
        foreach (explode("\r\n", $raw) as $line) {
            $pos = strpos($line, ':');
            if ($pos !== false) {
                $out[strtolower(trim(substr($line, 0, $pos)))] = trim(substr($line, $pos + 1));
            }
        }

        return $out;
    }
}
