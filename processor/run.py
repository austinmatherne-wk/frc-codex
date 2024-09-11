import logging
import multiprocessing
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
logging.basicConfig(level=logging.INFO, format='{%(processName)s} [%(levelname)s] %(message)s')


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

        processor_count = multiprocessing.cpu_count()
        if processor_options.maximum_processors > 0:
            processor_count = min(processor_count, processor_options.maximum_processors)

        temp_directories = {}
        for i in range(processor_count):
            temp_directories[i] = tempfile.TemporaryDirectory()

        temp_directory_paths = [Path(temp_dir.name) for temp_dir in temp_directories.values()]
        if cache_zip_downloaded:
            cache_manager.extract(temp_directory_paths)

        try:
            while True:
                next_sync_ts = datetime.now() + timedelta(seconds=processor_options.sync_interval_seconds)
                logger.info("Running processors. Next sync at: %s", next_sync_ts)
                processes = []
                for i, temp_dir in temp_directories.items():
                    cache_directory = Path(temp_dir.name)
                    process = multiprocessing.Process(
                        target=_run_processor,
                        args=(processor_options, cache_directory, next_sync_ts)
                    )
                    process.name = f'Processor-{i}'
                    process.start()
                    processes.append(process)
                for process in processes:
                    process.join()
                # Processors stopped to allow for syncing
                try:
                    paths_added = cache_manager.sync(temp_directory_paths)
                    if paths_added:
                        cache_manager.upload()
                    else:
                        logger.info("No cache changes to sync.")
                except Exception:
                    logger.exception('An unexpected error occurred while syncing the cache.')
        finally:
            for temp_dir in temp_directories.values():
                temp_dir.cleanup()


if __name__ == '__main__':
    main()
