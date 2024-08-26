import boto3


class QueueFactory:
    def __init__(
            self,
            sqs_endpoint_url,
            sqs_queue_name,
            sqs_region_name,
            sqs_visibility_timeout,
            aws_access_key_id,
            aws_secret_access_key,
    ):
        self._sqs_endpoint_url = sqs_endpoint_url
        self._sqs_queue_name = sqs_queue_name
        self._sqs_region_name = sqs_region_name
        self._sqs_visibility_timeout = sqs_visibility_timeout
        self._aws_access_key_id = aws_access_key_id
        self._aws_secret_access_key = aws_secret_access_key

    def get_queue(self):
        session = boto3.session.Session()
        client = session.resource(
            'sqs',
            region_name=self._sqs_region_name,
            aws_access_key_id=self._aws_access_key_id,
            aws_secret_access_key=self._aws_secret_access_key,
            endpoint_url=self._sqs_endpoint_url
        )
        attributes = {
            'VisibilityTimeout': str(self._sqs_visibility_timeout),
        }
        queue = client.create_queue(
            QueueName=self._sqs_queue_name,
            Attributes=attributes,
        )
        return queue
