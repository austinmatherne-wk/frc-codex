from abc import abstractmethod
from pathlib import Path


class CacheManager:

    @abstractmethod
    def download(self) -> bool:
        pass

    @abstractmethod
    def extract(self, cache_directories: list[Path]):
        pass

    @abstractmethod
    def sync(self, cache_directories: list[Path]) -> set[str]:
        pass

    @abstractmethod
    def upload(self) -> None:
        pass
