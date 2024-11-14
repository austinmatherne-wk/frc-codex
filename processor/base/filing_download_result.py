from dataclasses import dataclass
from pathlib import Path


@dataclass
class FilingDownloadResult:
    download_path: Path
    namelist: list[str]
