from typing import Iterable

from processor.base.queue_manager import QueueManager, JobMessage, ResultMessage


class MockQueueManager(QueueManager):

    def __init__(
            self,
            job_messages: list[JobMessage],
    ):
        self.job_messages = job_messages
        self.result_messages: list[ResultMessage] = []

    def complete_job(self, job_message: JobMessage) -> None:
        self.job_messages.remove(job_message)

    def get_jobs(self) -> Iterable[JobMessage]:
        for job_message in list(self.job_messages):
            yield job_message

    def publish_result(self, result_message: ResultMessage) -> None:
        self.result_messages.append(result_message)
