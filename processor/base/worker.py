from abc import abstractmethod
from dataclasses import dataclass
from pathlib import Path

from processor.base.queue_manager import JobMessage


@dataclass(frozen=True)
class WorkerResult:
    error: str = ''
    logs: str = ''
    viewer_entrypoint: str = ''
    success: bool = False


class Worker:

    @abstractmethod
    def work(self, job_message: JobMessage, target_path: Path, viewer_directory: Path) -> WorkerResult:
        pass
