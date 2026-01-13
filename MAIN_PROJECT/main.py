import asyncio
import pandas as pd
from core.environment import Environment
from core.message import Message

from agents.coordinator_agent import CoordinatorAgent
from agents.hypothesis_agent import HypothesisAgent
from agents.data_agent import DataAgent
from agents.transformation_agent import TransformationAgent
from agents.evaluator_agent import EvaluatorAgent
from agents.memory_agent import MemoryAgent


async def main():
    env = Environment()

    input_df = pd.read_csv("data/examples/example_2_input.csv")
    output_df = pd.read_csv("data/examples/example_2_output.csv")

    print("\n=== ВХОДНАЯ ТАБЛИЦА ===")
    print(input_df)
    print("\n=== ОЖИДАЕМЫЙ РЕЗУЛЬТАТ ===")
    print(output_df)

    env.register_agent(CoordinatorAgent("Coordinator", env))
    env.register_agent(DataAgent("DataAgent", env, "data/examples/example_2_input.csv"))
    env.register_agent(HypothesisAgent("HypothesisAgent", env))
    env.register_agent(TransformationAgent("TransformationAgent", env))
    env.register_agent(EvaluatorAgent("EvaluatorAgent", env))
    env.register_agent(MemoryAgent("MemoryAgent", env))

    await env.deliver_message(
        Message(
            sender="system",
            receiver="Coordinator",
            content=output_df,
            type="system_start"
        )
    )

    for _ in range(5):
        await env.step()
        await asyncio.sleep(0.1)


if __name__ == "__main__":
    asyncio.run(main())
