from core.agent_base import AgentBase
from utils.logger import Logger

class CoordinatorAgent(AgentBase):
    def __init__(self, name, environment):
        super().__init__(name, environment)
        self.input_df = None
        self.expected_df = None

    async def receive_message(self, message):

        if message.type == "system_start":
            self.expected_df = message.content
            Logger.log(self.name, "[control_container] Система запущена. Запрашиваю данные")
            
            await self.send_message("EvaluatorAgent", self.expected_df.copy(), "set_expected")

            await self.send_message("DataAgent", None, "request_data")

        elif message.type == "data_response":
            self.input_df = message.content
            Logger.log(self.name, "[control_container] Данные получены → HypothesisAgent")
            await self.send_message(
                "HypothesisAgent",
                (self.input_df.copy(), self.expected_df.copy()),
                "data_for_hypothesis"
            )

        elif message.type == "hypothesis_pipeline":
            Logger.log(self.name, "[control_container] Пайплайн получен → TransformationAgent")
            await self.send_message(
                "TransformationAgent",
                (self.input_df.copy(), message.content),
                "apply_pipeline"
            )

        elif message.type == "pipeline_result":
            pipeline, result_df = message.content
            Logger.log(self.name, "[control_container] Таблица после трансформаций:")
            print(result_df)
            await self.send_message(
                "EvaluatorAgent",
                (pipeline, result_df, self.expected_df.copy()),
                "evaluate_pipeline"
            )

        elif message.type == "evaluation_done":
            Logger.log(
                self.name,
                f"[control_container] Процесс завершён: "
                f"{'SUCCESS' if message.content else 'FAIL'}"
            )
