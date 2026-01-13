import pandas as pd
from core.agent_base import AgentBase
from utils.logger import Logger


class DataAgent(AgentBase):
    def __init__(self, name, environment, csv_path: str):
        super().__init__(name, environment)
        self.csv_path = csv_path
        self.dataframe = None

    def load_data(self):
        self.dataframe = pd.read_csv(self.csv_path)
        Logger.log(
            self.name,
            f"Данные загружены из '{self.csv_path}' "
            f"(строк={len(self.dataframe)}, столбцов={len(self.dataframe.columns)})"
        )

    async def receive_message(self, message):
        if message.type == "request_data":
            Logger.log(self.name, f"Получен запрос данных от {message.sender}")

            if self.dataframe is None:
                self.load_data()

            await self.send_message(
                message.sender,
                self.dataframe.copy(),
                "data_response"
            )

            Logger.log(self.name, f"Отправлена копия данных {message.sender}")
