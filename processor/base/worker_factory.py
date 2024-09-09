from abc import abstractmethod

from processor.base.worker import Worker


class WorkerFactory:

    @abstractmethod
    def create_worker(self, message) -> Worker:
        pass
