from core.agent_base import AgentBase
from transformations.drop_column import DropColumnTransformation
from transformations.rename_column import RenameColumnTransformation
from utils.logger import Logger

class HypothesisAgent(AgentBase):
    async def receive_message(self, message):
        if message.type != "data_for_hypothesis":
            return

        input_df, expected_df = message.content

        tried = set()
        pipeline = self.generate_pipeline(input_df, expected_df, tried)

        if not pipeline:
            Logger.log(self.name, "Новые гипотезы отсутствуют")
            return

        Logger.log(self.name, f"Предлагаю пайплайн: {[str(t) for t in pipeline]}")

        await self.send_message(
            "Coordinator",
            pipeline,
            "hypothesis_pipeline"
        )

    def generate_pipeline(self, input_df, expected_df, tried):
        pipeline = []

        input_cols = set(input_df.columns)
        expected_cols = set(expected_df.columns)

        # Найти колонки для rename
        rename_map = {}
        for in_col in input_cols:
            for out_col in expected_cols:
                if in_col == out_col or out_col in rename_map.values():
                    continue
                if input_df[in_col].equals(expected_df[out_col]):
                    rename_map[in_col] = out_col
                    break

        # Удаляем лишние колонки
        for col in input_cols:
            if col not in expected_cols and col not in rename_map:
                pipeline.append(DropColumnTransformation(col))

        # rename трансформации 
        for old, new in rename_map.items():
            pipeline.append(RenameColumnTransformation(old, new))

        # Проверка уникальности пайплайна
        serialized = tuple(type(t).__name__ + str(vars(t)) for t in pipeline)
        if serialized in tried:
            return None
        tried.add(serialized)

        return pipeline
