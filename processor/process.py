import logging
import os

from .queue_factory import QueueFactory
from .queue_manager import QueueManager

LOCAL_AWS_ACCESS_KEY_ID = 'local_aws_access_key'
LOCAL_AWS_SECRET_ACCESS_KEY = 'local_aws_secret_key'


logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format='%(message)s')


def _handle_message(message):
    pass


def main():
    sqs_endpoint_url = os.getenv('SQS_ENDPOINT_URL')
    assert sqs_endpoint_url, 'SQS_ENDPOINT_URL environment variable is required'
    sqs_queue_name = os.getenv('SQS_QUEUE_NAME')
    assert sqs_queue_name, 'SQS_QUEUE_NAME environment variable is required'
    sqs_region_name = os.getenv('SQS_REGION_NAME')
    assert sqs_region_name, 'SQS_REGION_NAME environment variable is required'

    sqs_max_messages = os.getenv('SQS_MAX_MESSAGES', 1)
    sqs_visibility_timeout = os.getenv('SQS_VISIBILITY_TIMEOUT', 60)
    sqs_wait_time = os.getenv('SQS_WAIT_TIME', 20)

    logger.info(
        f'Starting Arelle processor with '
        f'sqs_queue_name={sqs_queue_name}, '
        f'sqs_endpoint_url={sqs_endpoint_url}, '
        f'sqs_region_name={sqs_region_name}'
    )

    queue_factory = QueueFactory(
        sqs_endpoint_url=sqs_endpoint_url,
        sqs_queue_name=sqs_queue_name,
        sqs_region_name=sqs_region_name,
        sqs_visibility_timeout=sqs_visibility_timeout,
        aws_access_key_id=LOCAL_AWS_ACCESS_KEY_ID,
        aws_secret_access_key=LOCAL_AWS_SECRET_ACCESS_KEY,
    )
    queue = queue_factory.get_queue()
    queue_manager = QueueManager(queue, sqs_max_messages, sqs_wait_time)
    while True:
        for message in queue_manager.receive_messages():
            _handle_message(message)


if __name__ == '__main__':
    main()
