from pathlib import Path

from processor.base.job_message import JobMessage
from processor.base.worker import Worker, WorkerResult


class MockWorker(Worker):

    def __init__(self, worker_results_map: dict[str, WorkerResult]):
        self._worker_results_map = worker_results_map

    def work(
            self,
            job_message: JobMessage,
            target_path: Path,
            viewer_directory: Path,
            taxonomy_package_urls: list[str]
    ) -> WorkerResult:
        return self._worker_results_map[job_message.filing_id]
