from core.agent_base import AgentBase
from utils.logger import Logger
import pandas as pd

class EvaluatorAgent(AgentBase):
    def __init__(self, name, environment):
        super().__init__(name, environment)
        self.expected_df = None  

    async def receive_message(self, message):
        if message.type == "set_expected":
            self.expected_df = message.content
            Logger.log(self.name, "Ожидаемая таблица установлена")
            return

        if message.type != "evaluate_pipeline":
            return

        pipeline, result_df, expected_df = message.content

        expected_df = expected_df if expected_df is not None else self.expected_df

        success = result_df.equals(expected_df)
        Logger.log(self.name, f"Результат оценки: {'УСПЕХ' if success else 'НЕУДАЧА'}")

        await self.send_message("Coordinator", success, "evaluation_done")
        await self.send_message("MemoryAgent", (pipeline, success), "memory_update")
