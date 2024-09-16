import os
from functools import cached_property

LOCAL_AWS_ACCESS_KEY_ID = 'local_aws_access_key'
LOCAL_AWS_SECRET_ACCESS_KEY = 'local_aws_secret_key'


class ProcessorOptions:

    def __init__(self, secrets_filepath):
        self.secrets_filepath = secrets_filepath
        with open(secrets_filepath) as f:
            self._secrets = {
                key.strip(): value.strip()
                for key, value in [
                    line.split('=') for line in f.readlines()
                ]
            }

    @property
    def aws_access_key_id(self):
        return LOCAL_AWS_ACCESS_KEY_ID

    @property
    def aws_secret_access_key(self):
        return LOCAL_AWS_SECRET_ACCESS_KEY

    @property
    def companies_house_rest_api_key(self):
        return self._secrets['COMPANIES_HOUSE_REST_API_KEY']

    @cached_property
    def maximum_processors(self):
        return int(os.getenv('MAXIMUM_PROCESSORS', 0))

    @cached_property
    def s3_http_cache_bucket_name(self):
        return os.getenv('S3_HTTP_CACHE_BUCKET_NAME')

    @cached_property
    def s3_results_bucket_name(self):
        return os.getenv('S3_RESULTS_BUCKET_NAME')

    @cached_property
    def s3_endpoint_url(self):
        return os.getenv('S3_ENDPOINT_URL')

    @cached_property
    def s3_region_name(self):
        return os.getenv('S3_REGION_NAME')

    @cached_property
    def sqs_jobs_queue_name(self):
        return 'frc_codex_jobs'

    @cached_property
    def sqs_results_queue_name(self):
        return 'frc_codex_results'

    @cached_property
    def sqs_endpoint_url(self):
        return os.getenv('SQS_ENDPOINT_URL')

    @cached_property
    def sqs_region_name(self):
        return os.getenv('SQS_REGION_NAME')

    @cached_property
    def sqs_max_messages(self):
        return os.getenv('SQS_MAX_MESSAGES', 1)

    @cached_property
    def sqs_visibility_timeout(self):
        return os.getenv('SQS_VISIBILITY_TIMEOUT', 60)

    @cached_property
    def sqs_wait_time(self):
        return os.getenv('SQS_WAIT_TIME', 20)

    @cached_property
    def sync_interval_seconds(self):
        return os.getenv('SYNC_INTERVAL_SECONDS', 0)
