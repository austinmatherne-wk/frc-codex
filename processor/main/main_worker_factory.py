from processor.base.worker import Worker
from processor.base.worker_factory import WorkerFactory
from processor.main.workers.ixbrl_viewer_worker import IxbrlViewerWorker


class MainWorkerFactory(WorkerFactory):

    def create_worker(self, message) -> Worker:
        return IxbrlViewerWorker()
