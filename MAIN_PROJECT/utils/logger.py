from datetime import datetime


class Logger:
    COLORS = {
        "Coordinator": "\033[94m",          
        "HypothesisAgent": "\033[92m",      
        "TransformationAgent": "\033[93m",  
        "EvaluatorAgent": "\033[91m",       
        "MemoryAgent": "\033[95m",          
        "DataAgent": "\033[96m",            
        "END": "\033[0m"
    }

    AGENT_CONTAINERS = {
        # Контейнер управления
        "Coordinator": "control_container",

        # Контейнер данных / анализа
        "DataAgent": "data_container",
        "HypothesisAgent": "data_container",

        # Контейнер обработки и оценки
        "TransformationAgent": "processing_container",
        "EvaluatorAgent": "processing_container",
        "MemoryAgent": "processing_container",
    }

    @staticmethod
    def log(agent_name: str, message: str, receiver: str = None):

        timestamp = datetime.now().strftime("%H:%M:%S")
        color = Logger.COLORS.get(agent_name, "")
        end = Logger.COLORS["END"]

        container = Logger.AGENT_CONTAINERS.get(
            agent_name, "unknown_container"
        )

        container_tag = f"[{container}]"

        if receiver:
            print(
                f"{color}[{timestamp}] {container_tag} "
                f"[{agent_name}] → [{receiver}]: {message}{end}"
            )
        else:
            print(
                f"{color}[{timestamp}] {container_tag} "
                f"[{agent_name}]: {message}{end}"
            )
