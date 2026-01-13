# core/agent_base.py
import asyncio
from abc import ABC, abstractmethod
from core.message import Message
from utils.logger import Logger

class AgentBase(ABC):
    def __init__(self, name, environment):
        self.name = name
        self.environment = environment

    async def send_message(self, receiver, content, msg_type):
        Logger.log(self.name, f"send {msg_type}", receiver)
        await self.environment.deliver_message(
            Message(self.name, receiver, content, msg_type)
        )

    async def run(self, inbox: asyncio.Queue):
        Logger.log(self.name, "Agent started")
        while True:
            msg = await inbox.get()
            await self.receive_message(msg)

    @abstractmethod
    async def receive_message(self, message: Message):
        pass
