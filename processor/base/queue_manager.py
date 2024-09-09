import datetime
from abc import abstractmethod
from dataclasses import dataclass
from typing import Iterable


@dataclass(frozen=True)
class JobMessage:
    filing_id: str
    download_url: str
    registry_code: str
    receipt_handle: str
    message_id: str


@dataclass(frozen=True)
class ResultMessage:
    company_name: str | None
    company_number: str | None
    document_date: datetime.datetime | None
    error: str | None
    filing_id: str
    logs: str
    success: bool
    viewer_entrypoint: str | None


class QueueManager:
    @abstractmethod
    def complete_job(self, job_message: JobMessage) -> None:
        pass

    @abstractmethod
    def get_jobs(self) -> Iterable[JobMessage]:
        pass

    @abstractmethod
    def publish_result(self, result_message: ResultMessage) -> None:
        pass
