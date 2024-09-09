from abc import abstractmethod
from pathlib import Path


class UploadManager:

    @abstractmethod
    def upload_files(self, filing_id: str, viewer_directory: Path) -> None:
        pass
