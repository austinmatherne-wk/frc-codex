from pathlib import Path

from processor.base.worker import Worker
from processor.base.worker_factory import WorkerFactory
from processor.main.workers.ixbrl_viewer_worker import IxbrlViewerWorker
from processor.processor_options import ProcessorOptions


class MainWorkerFactory(WorkerFactory):

    def __init__(self, processor_options: ProcessorOptions, http_cache_directory: Path | None = None):
        self._processor_options = processor_options
        self._http_cache_directory = http_cache_directory

    def create_worker(self, message) -> Worker:
        return IxbrlViewerWorker(self._processor_options, self._http_cache_directory)
