from abc import ABC, abstractmethod
import pandas as pd


class Transformation(ABC):
    @abstractmethod
    def apply(self, df: pd.DataFrame) -> pd.DataFrame:
        pass
