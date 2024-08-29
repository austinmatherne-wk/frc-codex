#!/bin/sh


echo "Initializing localstack SQS: frc_codex_jobs"
awslocal sqs create-queue --queue-name frc_codex_jobs --region "$SQS_REGION_NAME"
echo "Initializing localstack SQS: frc_codex_results"
awslocal sqs create-queue --queue-name frc_codex_results --region "$SQS_REGION_NAME"
