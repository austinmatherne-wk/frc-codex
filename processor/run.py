import logging
import os
import tempfile
from datetime import datetime, timedelta
from pathlib import Path

from processor.main.main_cache_manager import MainCacheManager
from processor.main.main_download_manager import MainDownloadManager
from processor.main.main_queue_manager import MainQueueManager
from processor.main.main_upload_manager import MainUploadManager
from processor.main.main_worker_factory import MainWorkerFactory
from processor.processor import Processor
from processor.processor_options import ProcessorOptions

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format='%(message)s')


def _run_processor(processor_options: ProcessorOptions, cache_directory: Path, next_sync_ts: datetime) -> None:
    processor = Processor(
        download_manager=MainDownloadManager(processor_options),
        queue_manager=MainQueueManager(processor_options),
        upload_manager=MainUploadManager(processor_options),
        worker_factory=MainWorkerFactory(cache_directory),
    )
    while True:
        processor.run()
        if datetime.now() >= next_sync_ts:
            break


def main():
    secrets_filepath = os.getenv('SECRETS_FILEPATH')
    processor_options = ProcessorOptions(secrets_filepath)

    with tempfile.TemporaryDirectory() as global_dir:
        cache_zip_path = Path(global_dir) / '_HTTP_CACHE.zip'
        cache_manager = MainCacheManager(processor_options, cache_zip_path)
        cache_zip_downloaded = cache_manager.download()

        temp_dir = tempfile.TemporaryDirectory()
        cache_directory = Path(temp_dir.name)
        if cache_zip_downloaded:
            cache_manager.extract([cache_directory])

        try:
            while True:
                next_sync_ts = datetime.now() + timedelta(seconds=processor_options.sync_interval_seconds)
                logger.info("Running processor. Next sync at: %s", next_sync_ts)
                _run_processor(processor_options, cache_directory, next_sync_ts)
                # Processor stopped to allow for syncing
                try:
                    paths_added = cache_manager.sync([cache_directory])
                    if paths_added:
                        cache_manager.upload()
                    else:
                        logger.info("No cache changes to sync.")
                except Exception:
                    logger.exception('An unexpected error occurred while syncing the cache.')
        finally:
            temp_dir.cleanup()


if __name__ == '__main__':
    main()
