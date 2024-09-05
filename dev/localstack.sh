#!/bin/sh

# Create SQS Queues
echo "Initializing localstack SQS: frc_codex_jobs"
awslocal sqs create-queue --queue-name frc_codex_jobs --region "$SQS_REGION_NAME"
echo "Initializing localstack SQS: frc_codex_results"
awslocal sqs create-queue --queue-name frc_codex_results --region "$SQS_REGION_NAME"

# Create S3 Bucket
echo "Initializing localstack S3: $S3_BUCKET_NAME"
awslocal s3 mb "s3://$S3_BUCKET_NAME" --region "$S3_REGION_NAME"
