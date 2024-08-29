import logging
import os

from .queue_factory import QueueFactory
from .queue_manager import QueueManager

LOCAL_AWS_ACCESS_KEY_ID = 'local_aws_access_key'
LOCAL_AWS_SECRET_ACCESS_KEY = 'local_aws_secret_key'


logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format='%(message)s')


def _handle_message(message, jobs_queue, results_queue):
    filing_id = message.body
    stub_viewer_url = message.message_attributes.get('downloadUrl').get('StringValue')
    # TODO: Use Arelle to actually process filing, temporarily just fake success.
    success = True
    if not success:
        # TODO: Handle Arelle failure
        return
    logger.info("Processing successful for message: (%s) %s", message.message_id, message.body)
    results_queue.send_message(
        MessageBody=filing_id,
        MessageAttributes={
            'stubViewerUrl': {
                'StringValue': stub_viewer_url,
                'DataType': 'String'
            },
            'success': {
                'StringValue': 'true' if success else 'false',
                'DataType': 'String'
            },
        }
    )
    jobs_queue.delete_messages(Entries=[{'Id': message.message_id, 'ReceiptHandle': message.receipt_handle}])


def main():
    sqs_jobs_queue_name = 'frc_codex_jobs'
    sqs_results_queue_name = 'frc_codex_results'

    sqs_endpoint_url = os.getenv('SQS_ENDPOINT_URL')
    assert sqs_endpoint_url, 'SQS_ENDPOINT_URL environment variable is required'
    sqs_region_name = os.getenv('SQS_REGION_NAME')
    assert sqs_region_name, 'SQS_REGION_NAME environment variable is required'

    sqs_max_messages = os.getenv('SQS_MAX_MESSAGES', 1)
    sqs_visibility_timeout = os.getenv('SQS_VISIBILITY_TIMEOUT', 60)
    sqs_wait_time = os.getenv('SQS_WAIT_TIME', 20)

    logger.info(
        f'Starting Arelle processor with '
        f'sqs_jobs_queue_name={sqs_jobs_queue_name}, '
        f'sqs_results_queue_name={sqs_results_queue_name}, '
        f'sqs_endpoint_url={sqs_endpoint_url}, '
        f'sqs_region_name={sqs_region_name}'
    )

    queue_factory = QueueFactory(
        sqs_endpoint_url=sqs_endpoint_url,
        sqs_jobs_queue_name=sqs_jobs_queue_name,
        sqs_results_queue_name=sqs_results_queue_name,
        sqs_region_name=sqs_region_name,
        sqs_visibility_timeout=sqs_visibility_timeout,
        aws_access_key_id=LOCAL_AWS_ACCESS_KEY_ID,
        aws_secret_access_key=LOCAL_AWS_SECRET_ACCESS_KEY,
    )
    jobs_queue = queue_factory.get_jobs_queue()
    results_queue = queue_factory.get_results_queue()
    queue_manager = QueueManager(jobs_queue, sqs_max_messages, sqs_wait_time)
    while True:
        for message in queue_manager.receive_messages():
            _handle_message(message, jobs_queue, results_queue)


if __name__ == '__main__':
    main()
