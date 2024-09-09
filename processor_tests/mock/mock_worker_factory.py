from processor.base.worker import Worker, WorkerResult
from processor.base.worker_factory import WorkerFactory
from processor_tests.mock.workers.mock_worker import MockWorker


class MockWorkerFactory(WorkerFactory):

    def __init__(self, worker_results_map: dict[str, WorkerResult]):
        self._worker_results_map = worker_results_map

    def create_worker(self, message) -> Worker:
        return MockWorker(self._worker_results_map)
