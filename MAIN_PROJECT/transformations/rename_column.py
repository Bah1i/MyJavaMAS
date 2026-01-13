class RenameColumnTransformation:
    def __init__(self, old_name: str, new_name: str):
        self.old_name = old_name
        self.new_name = new_name

    def apply(self, df):
        from utils.logger import Logger
        if self.old_name in df.columns:
            Logger.log("TransformationAgent", f"Переименовываю колонку '{self.old_name}' → '{self.new_name}'")
            return df.rename(columns={self.old_name: self.new_name})
        return df

    def __str__(self):
        return f"RenameColumn('{self.old_name}' → '{self.new_name}')"
