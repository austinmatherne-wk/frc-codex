from pathlib import Path
from unittest import TestCase
from unittest.mock import Mock, patch

from processor.base.queue_manager import JobMessage, ResultMessage
from processor.base.worker import WorkerResult
from processor.processor import Processor
from processor_tests.mock.mock_queue_manager import MockQueueManager
from processor_tests.mock.mock_upload_manager import MockUploadManager
from processor_tests.mock.mock_worker_factory import MockWorkerFactory


class TestProcessor(TestCase):

    @patch('processor.processor.Processor._get_target_path')
    def test_processor_run_success(self, mock_get_target_path) -> None:
        job_messages = [
            JobMessage(
                filing_id='filing_id1',
                registry_code='registry_code1',
                download_url='download_url1',
                receipt_handle='receipt_handle1',
                message_id='message_id1',
            ),
            JobMessage(
                filing_id='filing_id2',
                registry_code='registry_code2',
                download_url='download_url2',
                receipt_handle='receipt_handle2',
                message_id='message_id2',
            ),
        ]
        worker_results_map = {
            'filing_id1': WorkerResult(
                logs='logs1',
                success=True,
                viewer_entrypoint='viewer_entrypoint1',
            ),
            'filing_id2': WorkerResult(
                error='error2',
                logs='logs2',
                success=False,
            ),
        }
        mock_get_target_path.return_value = (Path('download_url1'), [])
        queue_manager = MockQueueManager(job_messages=job_messages)
        upload_manager = MockUploadManager()
        processor = Processor(
            download_manager=Mock(),
            queue_manager=queue_manager,
            upload_manager=upload_manager,
            worker_factory=MockWorkerFactory(worker_results_map=worker_results_map),
        )
        worker_results = processor.run()

        self.assertEqual(worker_results, [
            WorkerResult(
                logs='logs1',
                success=True,
                viewer_entrypoint='viewer_entrypoint1',
            ),
            WorkerResult(
                error='error2',
                logs='logs2',
                success=False,
            )
        ])
        self.assertEqual(queue_manager.result_messages, [
            ResultMessage(
                error='',
                filing_id='filing_id1',
                logs='logs1',
                success=True,
                viewer_entrypoint='viewer_entrypoint1',
            ),
            ResultMessage(
                error='error2',
                filing_id='filing_id2',
                logs='logs2',
                success=False,
                viewer_entrypoint='',
            ),
        ])
        self.assertEqual(upload_manager.uploads, [
            ('filing_id1', 'viewer'),
        ])
