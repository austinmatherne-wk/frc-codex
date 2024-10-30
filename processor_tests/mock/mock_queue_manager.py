from typing import Iterable

from processor.base.job_message import JobMessage
from processor.base.queue_manager import QueueManager
from processor.base.worker import WorkerResult


class MockQueueManager(QueueManager):

    def __init__(
            self,
            job_messages: list[JobMessage],
    ):
        self.job_messages = job_messages
        self.worker_results: list[WorkerResult] = []

    def complete_job(self, job_message: JobMessage) -> None:
        self.job_messages.remove(job_message)

    def get_jobs(self) -> Iterable[JobMessage]:
        yield from list(self.job_messages)

    def publish_result(self, result_message: WorkerResult) -> None:
        self.worker_results.append(result_message)
