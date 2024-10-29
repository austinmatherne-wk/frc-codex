from pathlib import Path

from processor.base.upload_manager import UploadManager


class MockUploadManager(UploadManager):

    def __init__(self):
        self.uploads = []

    def upload_files(self, filing_id: str, viewer_directory: Path) -> int:
        self.uploads.append((filing_id, viewer_directory.name))
        return 1024
