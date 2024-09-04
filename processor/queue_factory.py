import boto3


class QueueFactory:
    def __init__(
            self,
            sqs_endpoint_url,
            sqs_jobs_queue_name,
            sqs_results_queue_name,
            sqs_region_name,
            sqs_visibility_timeout,
            aws_access_key_id,
            aws_secret_access_key,
    ):
        self._sqs_endpoint_url = sqs_endpoint_url
        self._sqs_jobs_queue_name = sqs_jobs_queue_name
        self._sqs_results_queue_name = sqs_results_queue_name
        self._sqs_region_name = sqs_region_name
        self._sqs_visibility_timeout = sqs_visibility_timeout
        self._aws_access_key_id = aws_access_key_id
        self._aws_secret_access_key = aws_secret_access_key

    def _get_queue(self, queue_name):
        session = boto3.session.Session()
        client = session.resource(
            'sqs',
            region_name=self._sqs_region_name,
            aws_access_key_id=self._aws_access_key_id,
            aws_secret_access_key=self._aws_secret_access_key,
            endpoint_url=self._sqs_endpoint_url
        )
        queue = client.get_queue_by_name(QueueName=queue_name)
        return queue

    def get_jobs_queue(self):
        return self._get_queue(self._sqs_jobs_queue_name)

    def get_results_queue(self):
        return self._get_queue(self._sqs_results_queue_name)
