from pathlib import Path

from processor.base.worker import Worker
from processor.base.worker_factory import WorkerFactory
from processor.main.workers.ixbrl_viewer_worker import IxbrlViewerWorker


class MainWorkerFactory(WorkerFactory):

    def __init__(self, cache_directory: Path):
        self._cache_directory = cache_directory

    def create_worker(self, message) -> Worker:
        return IxbrlViewerWorker(self._cache_directory)
