import asyncio
from typing import Dict
from core.message import Message


class Environment:
    def __init__(self):
        self.agents: Dict[str, object] = {}
        self.queue = asyncio.Queue()

    def register_agent(self, agent):
        self.agents[agent.name] = agent

    async def deliver_message(self, message: Message):
        await self.queue.put(message)

    async def dispatch_messages(self):
        while not self.queue.empty():
            message = await self.queue.get()
            receiver = self.agents.get(message.receiver)
            if receiver:
                await receiver.receive_message(message)

    async def step(self):
        await self.dispatch_messages()
