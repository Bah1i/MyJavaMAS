from core.agent_base import AgentBase
from utils.logger import Logger


class MemoryAgent(AgentBase):
    def __init__(self, name, environment):
        super().__init__(name, environment)
        self.successful = []
        self.failed = []

    async def receive_message(self, message):
        if message.type == "memory_update":
            pipeline, success = message.content

            if success:
                self.successful.append(pipeline)
                Logger.log(self.name, "Сохранен успешный пайплайн")
            else:
                self.failed.append(pipeline)
                Logger.log(self.name, "Сохранен неудачный пайплайн")

        elif message.type == "request_memory":
            await self.send_message(
                message.sender,
                {"successful": self.successful, "failed": self.failed},
                "memory_response"
            )
