from abc import abstractmethod
from typing import Iterable

from processor.base.job_message import JobMessage
from processor.base.worker import WorkerResult


class QueueManager:
    @abstractmethod
    def complete_job(self, job_message: JobMessage) -> None:
        pass

    @abstractmethod
    def get_jobs(self) -> Iterable[JobMessage]:
        pass

    @abstractmethod
    def publish_result(self, worker_result: WorkerResult) -> None:
        pass
