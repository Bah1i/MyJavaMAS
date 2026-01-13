from dataclasses import dataclass
from typing import Any


@dataclass
class Message:
    sender: str
    receiver: str
    content: Any
    type: str = "info"
