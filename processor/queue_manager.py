import logging

from botocore.exceptions import ClientError

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format='%(message)s')


class QueueManager:

    def __init__(self, queue, max_number, wait_time):
        self._queue = queue
        self._max_number = max_number
        self._wait_time = wait_time

    def receive_messages(self):
        """
        Receive a batch of messages in a single request from an SQS queue.
        :return: The list of Message objects received. These each contain the body
                 of the message and metadata and custom attributes.
        """
        try:
            logger.info("Waiting for messages...")
            messages = self._queue.receive_messages(
                MessageAttributeNames=["All"],
                MaxNumberOfMessages=self._max_number,
                WaitTimeSeconds=self._wait_time,
            )
            logger.info("Received %s message(s).", len(messages))
            for message in messages:
                logger.info("Processing started for message: (%s) %s", message.message_id, message.body)
                yield message
                logger.info("Processing completed for message: (%s) %s", message.message_id, message.body)
        except ClientError as error:
            logger.exception("Couldn't receive messages from queue: %s", self._queue)
            raise error
