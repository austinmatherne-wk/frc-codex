import logging
from typing import Iterable

import boto3
from botocore.exceptions import ClientError

from processor.processor_options import ProcessorOptions
from processor.base.queue_manager import QueueManager, ResultMessage, JobMessage

logger = logging.getLogger(__name__)


class MainQueueManager(QueueManager):

    def __init__(self, processor_options: ProcessorOptions):
        self._jobs_queue = MainQueueManager._get_queue(processor_options.sqs_jobs_queue_name, processor_options)
        self._results_queue = MainQueueManager._get_queue(processor_options.sqs_results_queue_name, processor_options)
        self._max_number = processor_options.sqs_max_messages
        self._wait_time = processor_options.sqs_wait_time

    def complete_job(self, job_message: JobMessage) -> None:
        self._jobs_queue.delete_messages(
            Entries=[{
                'Id': job_message.message_id,
                'ReceiptHandle': job_message.receipt_handle
            }]
        )

    def get_jobs(self) -> Iterable[JobMessage]:
        """
        Receive a batch of job messages in a single request from an SQS queue.
        :return: The list of Message objects received. These each contain the body
                 of the message and metadata and custom attributes.
        """
        try:
            logger.debug("Waiting for messages...")
            messages = self._jobs_queue.receive_messages(
                MessageAttributeNames=["All"],
                MaxNumberOfMessages=self._max_number,
                WaitTimeSeconds=self._wait_time,
            )
            logger.debug("Received %s message(s).", len(messages))
            for message in messages:
                attributes_names = [key for key in message.message_attributes] if message.message_attributes else []
                logger.info(
                    "Processing started for message with attributes %s: (%s) %s",
                    attributes_names, message.message_id, message.body
                )
                yield JobMessage(
                    download_url=message.message_attributes['DownloadUrl']['StringValue'],
                    filing_id=message.body,
                    message_id=message.message_id,
                    receipt_handle=message.receipt_handle,
                    registry_code=message.message_attributes['RegistryCode']['StringValue'],
                )
                logger.info("Processing completed for message: (%s) %s", message.message_id, message.body)
        except ClientError as error:
            logger.exception("Couldn't receive messages from queue: %s", self._jobs_queue)
            raise error

    def publish_result(self, result_message: ResultMessage) -> None:
        message_attributes = {
            'FilingId': {
                'StringValue': result_message.filing_id,
                'DataType': 'String'
            },
            'Success': {
                'StringValue': 'true' if result_message.success else 'false',
                'DataType': 'String'
            },
        }
        if result_message.company_name:
            message_attributes['CompanyName'] = {
                'StringValue': result_message.company_name,
                'DataType': 'String'
            }
        if result_message.company_number:
            message_attributes['CompanyNumber'] = {
                'StringValue': result_message.company_number,
                'DataType': 'String'
            }
        if result_message.document_date:
            message_attributes['DocumentDate'] = {
                'StringValue': result_message.document_date.strftime('%Y-%m-%d'),
                'DataType': 'String'
            }
        if result_message.error:
            message_attributes['Error'] = {
                'StringValue': result_message.error,
                'DataType': 'String'
            }
        if result_message.viewer_entrypoint:
            message_attributes['ViewerEntrypoint'] = {
                'StringValue': result_message.viewer_entrypoint,
                'DataType': 'String'
            }
        self._results_queue.send_message(
            MessageBody=result_message.logs,
            MessageAttributes=message_attributes
        )

    @staticmethod
    def _get_queue(queue_name: str, processor_options: ProcessorOptions):
        session = boto3.session.Session()
        client = session.resource(
            'sqs',
            region_name=processor_options.sqs_region_name,
            endpoint_url=processor_options.sqs_endpoint_url
        )
        queue = client.get_queue_by_name(QueueName=queue_name)
        return queue
