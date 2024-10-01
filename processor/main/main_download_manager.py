import logging
import re
from pathlib import Path
from urllib.parse import urlparse, parse_qs, urlunparse

import requests

from processor.base.download_manager import DownloadManager
from processor.processor_options import ProcessorOptions

logger = logging.getLogger(__name__)


class MainDownloadManager(DownloadManager):

    def __init__(self, processor_options: ProcessorOptions):
        self._processor_options = processor_options

    def _download_ch_filing(self, filing_id, download_url, directory) -> Path:
        download_url, content_type = self._split_ch_url(download_url)
        logger.info(
            "Downloading filing from CH: From %s (%s) to %s (Filing: %s)",
            download_url, content_type, directory, filing_id
        )
        response = self._retrieve(
            url=download_url,
            auth=(self._processor_options.companies_house_rest_api_key, ''),
            headers={'Accept': content_type}
        )
        filing_path = self._get_ch_download_path(directory, response)
        self._save(response, filing_path)
        return filing_path

    def _download_fca_filing(self, filing_id: str, download_url: str, directory: Path) -> Path:
        filing_path = directory / 'filing.zip'
        logger.info("Downloading filing from FCA: (%s) from %s to %s", filing_id, download_url, filing_path)
        response = self._retrieve(download_url, auth=None, headers=None)
        self._save(response, filing_path)
        return filing_path

    def _get_ch_download_path(self, directory, response):
        # Get original filename from: 'inline;filename="..."'
        content_disposition = response.headers['Content-Disposition']
        filename_match = re.search(r'filename="(.+)"', content_disposition)
        assert filename_match, f"Could not find filename in Content-Disposition: {content_disposition}"
        filename = filename_match.group(1)
        return directory / filename

    def _retrieve(self, url: str, auth: tuple[str, str] | None, headers: dict[str, str] | None) -> requests.Response:
        response = requests.get(
            url=url,
            auth=auth,
            headers=headers
        )
        response.raise_for_status()
        return response

    def _save(self, response: requests.Response, path: Path) -> None:
        with open(path, 'wb') as file:
            file.write(response.content)

    def _split_ch_url(self, download_url: str) -> tuple[str, str]:
        parsed_url = urlparse(download_url)
        query = parse_qs(parsed_url.query)
        assert 'contentType' in query, f'Missing contentType in download URL: {download_url}'
        content_type = query['contentType'][0]
        download_url = urlunparse(parsed_url._replace(query=""))
        return download_url, content_type

    def download_filing(self, filing_id: str, registry_code: str, download_url: str, directory: Path) -> Path:
        if registry_code == 'CH':
            return self._download_ch_filing(filing_id, download_url, directory)
        if registry_code == 'FCA':
            return self._download_fca_filing(filing_id, download_url, directory)
        raise ValueError(f"Unknown registry code: {registry_code}")
