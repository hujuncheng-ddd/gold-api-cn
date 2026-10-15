<?php

declare(strict_types=1);

namespace GoldApi\Sdk\Exception;

/**
 * GoldAPI 调用异常
 *
 * 携带平台错误码（msgId）、requestId 与 HTTP 状态码，
 * 便于调用方按错误码做分支处理与问题上报。
 */
class GoldApiException extends \RuntimeException
{
    /** @var string|null 平台错误码，如 DAILY_LIMIT_EXCEEDED；网络失败时为 null */
    private $msgId;

    /** @var string|null 平台 requestId；网络失败时为 null */
    private $requestId;

    /** @var int HTTP 状态码，网络失败时为 0 */
    private $httpCode;

    public function __construct(
        string $message,
        int $httpCode = 0,
        int $code = 0,
        ?string $msgId = null,
        ?string $requestId = null
    ) {
        parent::__construct($message, $code);
        $this->message   = $message;
        $this->httpCode  = $httpCode;
        $this->msgId     = $msgId;
        $this->requestId = $requestId;
    }

    /**
     * 从平台失败响应构造异常
     * 响应格式: {"success":"0","msgId":"...","msg":"...","requestId":"..."}
     */
    public static function fromResponse(array $data, int $httpCode): self
    {
        return new self(
            (string) ($data['msg'] ?? '未知错误'),
            $httpCode,
            0,
            isset($data['msgId']) ? (string) $data['msgId'] : null,
            isset($data['requestId']) ? (string) $data['requestId'] : null
        );
    }

    /** 平台错误码，如 INVALID_API_KEY、DAILY_LIMIT_EXCEEDED；网络失败时返回 null */
    public function getMsgId(): ?string
    {
        return $this->msgId;
    }

    /** 平台 requestId，排查问题时提供给平台方 */
    public function getRequestId(): ?string
    {
        return $this->requestId;
    }

    /** HTTP 状态码，网络失败时为 0 */
    public function getHttpCode(): int
    {
        return $this->httpCode;
    }

    /** 是否为配额 / 频率类错误（可退避重试或提醒用户升级配额） */
    public function isRateLimit(): bool
    {
        return \in_array($this->msgId, [
            'DAILY_LIMIT_EXCEEDED',
            'MONTHLY_LIMIT_EXCEEDED',
            'QPS_LIMIT_EXCEEDED',
            'VARIETY_QPS_EXCEEDED',
        ], true);
    }
}
