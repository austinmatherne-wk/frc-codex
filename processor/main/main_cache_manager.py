import logging
import shutil
import zipfile
from pathlib import Path

import boto3
from botocore.exceptions import ClientError

from processor.base.cache_manager import CacheManager
from processor.processor_options import ProcessorOptions


BACKUP_CACHE_ZIP_PATH = Path('/tmp/_HTTP_CACHE.zip')
CACHE_IGNORE_SUFFIXES = {'.lock', '.zip', '.tmp', '.DS_Store'}

logger = logging.getLogger(__name__)


class MainCacheManager(CacheManager):

    def __init__(self, processor_options: ProcessorOptions, cache_zip_path: Path):
        self._processor_options = processor_options
        self._cache_zip_path = cache_zip_path
        self._s3_client = boto3.client(
            's3',
            endpoint_url=self._processor_options.s3_endpoint_url,
            region_name=self._processor_options.s3_region_name,
            aws_access_key_id=self._processor_options.aws_access_key_id,
            aws_secret_access_key=self._processor_options.aws_secret_access_key,
        )

    def download(self) -> bool:
        """
        Download the HTTP cache from S3. If the cache is not found on S3, a backup cache is used, if it exists.
        :return: Whether a cache was downloaded (or copied from backup)
        """
        bucket_name = self._processor_options.s3_http_cache_bucket_name
        s3_path = self._cache_zip_path.name
        logger.info("Downloading HTTP cache: (%s) from %s: %s", self._cache_zip_path, bucket_name, s3_path)
        try:
            self._s3_client.download_file(
                bucket_name,
                s3_path,
                self._cache_zip_path
            )
        except ClientError as e:
            if e.response['Error']['Code'] != '404':
                return False
            logger.info("HTTP cache not found on S3")
            if not BACKUP_CACHE_ZIP_PATH.exists():
                return False
            logger.info("Copying backup cache from %s", BACKUP_CACHE_ZIP_PATH)
            shutil.copyfile(BACKUP_CACHE_ZIP_PATH, self._cache_zip_path)
        with zipfile.ZipFile(self._cache_zip_path, 'r') as zip_file:
            logger.debug("Initial HTTP cache: \n%s", '\n'.join(zip_file.namelist()))
        return True

    def extract(self, cache_directories: list[Path]) -> None:
        """
        Extract the HTTP cache to the given directories.
        :param cache_directories:
        :return:
        """
        for cache_directory in cache_directories:
            with zipfile.ZipFile(self._cache_zip_path, 'r') as zip_file:
                zip_file.extractall(cache_directory)

    def sync(self, cache_directories: list[Path]) -> set[str]:
        """
        Update the cache zip with new files from the given directories.
        :param cache_directories:
        :return: Set of added arcnames
        """
        paths_added = set()
        namelist = set()
        if self._cache_zip_path.exists():
            with zipfile.ZipFile(self._cache_zip_path, 'r') as zip_file:
                namelist.update(zip_file.namelist())
        with zipfile.ZipFile(self._cache_zip_path, 'a') as zip_file:
            for cache_directory in cache_directories:
                for cache_path in cache_directory.glob('**/*'):
                    if any(cache_path.suffix.endswith(suffix) for suffix in CACHE_IGNORE_SUFFIXES):
                        continue
                    if cache_path.is_dir():
                        continue
                    arcname = str(cache_path.relative_to(cache_directory))
                    if arcname in namelist:
                        continue
                    zip_file.write(cache_path, arcname)
                    paths_added.add(arcname)
                    namelist.add(arcname)
                    logger.info("Added to cache: %s (%s)", arcname, cache_path)
        return paths_added

    def upload(self) -> None:
        """
        Upload the updated cache zip to S3.
        :return:
        """
        bucket_name = self._processor_options.s3_http_cache_bucket_name
        s3_path = self._cache_zip_path.name
        with zipfile.ZipFile(self._cache_zip_path, 'r') as zip_file:
            new_namelist = zip_file.namelist()
        logger.debug("Files in updated cache: %s", '\n'.join(new_namelist))
        logger.info(
            "Added files to cache. Uploading HTTP cache: (%s) to %s: %s",
            self._cache_zip_path, bucket_name, s3_path
        )
        self._s3_client.upload_file(str(self._cache_zip_path), bucket_name, s3_path)
