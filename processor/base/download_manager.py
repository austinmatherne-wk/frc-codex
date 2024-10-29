from abc import abstractmethod
from pathlib import Path


class DownloadManager:

    @abstractmethod
    def download_filing(self, filing_id: str, registry_code: str, download_url: str, directory: Path) -> Path:
        pass

    @abstractmethod
    def get_package_urls(self) -> list[str]:
        pass
