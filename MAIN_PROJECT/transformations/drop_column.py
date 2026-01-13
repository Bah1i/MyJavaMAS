class DropColumnTransformation:
    def __init__(self, col_name: str):
        self.col_name = col_name

    def apply(self, df):
        from utils.logger import Logger
        if self.col_name in df.columns:
            Logger.log("TransformationAgent", f"Удаляю колонку '{self.col_name}'")
            return df.drop(columns=[self.col_name])
        return df

    def __str__(self):
        return f"DropColumn('{self.col_name}')"
