from core.agent_base import AgentBase
from utils.logger import Logger

class TransformationAgent(AgentBase):
    async def receive_message(self, message):
        if message.type != "apply_pipeline":
            return

        df, pipeline = message.content
        Logger.log(self.name, f"Применяю пайплайн длиной {len(pipeline)}")

        for i, transformation in enumerate(pipeline, 1):
            df = transformation.apply(df)
            Logger.log(self.name, f"  Трансформация {i}: {transformation}")

        await self.send_message(
            "Coordinator",
            (pipeline, df),
            "pipeline_result"
        )
