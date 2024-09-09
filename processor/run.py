import logging
import os

from processor.processor import Processor
from processor.processor_options import ProcessorOptions
from processor.main.main_download_manager import MainDownloadManager
from processor.main.main_queue_manager import MainQueueManager
from processor.main.main_upload_manager import MainUploadManager
from processor.main.main_worker_factory import MainWorkerFactory

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format='%(message)s')


if __name__ == '__main__':
    secrets_filepath = os.getenv('SECRETS_FILEPATH')
    processor_options = ProcessorOptions(secrets_filepath)
    processor = Processor(
        download_manager=MainDownloadManager(processor_options),
        queue_manager=MainQueueManager(processor_options),
        upload_manager=MainUploadManager(processor_options),
        worker_factory=MainWorkerFactory(),
    )
    while True:
        processor.run()
