#!/bin/sh

# Create SQS Queues
echo "Initializing localstack SQS: frc_codex_jobs"
awslocal sqs create-queue --queue-name frc_codex_jobs --region "$REGION_NAME"
echo "Initializing localstack SQS: frc_codex_results"
awslocal sqs create-queue --queue-name frc_codex_results --region "$REGION_NAME"

# Create S3 Bucket
echo "Initializing localstack S3 bucket: $S3_HTTP_CACHE_BUCKET_NAME"
awslocal s3 mb "s3://$S3_HTTP_CACHE_BUCKET_NAME" --region "$REGION_NAME"
echo "Initializing localstack S3 bucket: $S3_RESULTS_BUCKET_NAME"
awslocal s3 mb "s3://$S3_RESULTS_BUCKET_NAME" --region "$REGION_NAME"

# Create SES Identity
echo "Initializing localstack SES identity: $SES_EMAIL"
awslocal ses verify-email-identity \
  --email-address "$SUPPORT_EMAIL" \
  --region "$REGION_NAME"
