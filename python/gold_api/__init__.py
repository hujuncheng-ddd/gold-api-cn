"""gold-api —— GoldAPI (gold-api.cn) 贵金属行情 Python SDK。"""

from .client import GoldApiClient, DEFAULT_BASE_URL, MAX_IDS_PER_REQUEST
from .exceptions import GoldApiException

__version__ = "1.0.0"

__all__ = [
    "GoldApiClient",
    "GoldApiException",
    "DEFAULT_BASE_URL",
    "MAX_IDS_PER_REQUEST",
    "__version__",
]
