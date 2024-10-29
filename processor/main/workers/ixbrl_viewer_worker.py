import datetime
import logging
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import cast

from arelle.RuntimeOptions import RuntimeOptions  # type: ignore
from arelle.api.Session import Session  # type: ignore

from processor.base.job_message import JobMessage
from processor.base.worker import Worker, WorkerResult
from processor.processor_options import ProcessorOptions

VIEWER_HTML_FILENAME = 'ixbrlviewer.html'


logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class IxbrlViewerResult:
    success: bool
    logs: str
    company_name: str | None
    company_number: str | None
    document_date: datetime.datetime | None


class IxbrlViewerWorker(Worker):

    def __init__(self, processor_options: ProcessorOptions, http_cache_directory: Path | None = None):
        self._http_cache_directory = http_cache_directory or processor_options.http_cache_directory
        self._ixbrl_viewer_plugin_path = processor_options.ixbrl_viewer_plugin_path

    def work(
            self,
            job_message: JobMessage,
            target_path: Path,
            viewer_directory: Path,
            taxonomy_package_urls: list[str],
    ) -> WorkerResult:
        packages = list(taxonomy_package_urls)
        report_path = None
        for parent in target_path.parents:
            if parent.name == 'reports':
                report_path = parent
                continue
            if report_path and zipfile.is_zipfile(parent):
                packages.append(str(parent))
                break
        result = self._generate_viewer(target_path, viewer_directory, packages)
        if not result.success:
            return WorkerResult(
                job_message.filing_id,
                error='Viewer generation failed within Arelle. Check the logs for details.',
                logs=result.logs
            )
        viewer_path = viewer_directory / VIEWER_HTML_FILENAME
        if not viewer_path.exists():
            return WorkerResult(
                job_message.filing_id,
                error='Arelle reported success but viewer was not found. Check the logs for details.',
                logs=result.logs
            )
        return WorkerResult(
            job_message.filing_id,
            success=True,
            viewer_entrypoint=VIEWER_HTML_FILENAME,
            logs=result.logs,
            company_name=result.company_name,
            company_number=result.company_number,
            document_date=result.document_date,
        )

    def _get_value_by_local_name(self, model_xbrl, local_name: str) -> str | None:
        facts = model_xbrl.factsByLocalName.get(local_name, [])
        if facts:
            return next(iter(facts)).xValue
        return None

    def _generate_viewer(self, target_path: Path, viewer_directory: Path, packages: list[str]) -> IxbrlViewerResult:
        runtime_options = RuntimeOptions(
            cacheDirectory=str(self._http_cache_directory),
            disablePersistentConfig=True,
            entrypointFile=str(target_path),
            internetLogDownloads=True,
            internetRecheck='never',
            keepOpen=True,
            logFormat="[%(messageCode)s] %(message)s - %(file)s",
            logFile='logToBuffer',
            packages=packages,
            pluginOptions={
                'saveViewerDest': str(viewer_directory),
                'useStubViewer': True,
                'viewerNoCopyScript': True,
                'viewerURL': '/ixbrlviewer.js',
                'viewer_feature_home-link-label': 'FRC CODEx Filing Index',
                'viewer_feature_home-link-url': '/',
                'viewer_feature_support-link': '/help',
                'viewer_feature_survey-link': '/survey',
            },
            plugins=str(self._ixbrl_viewer_plugin_path),
            strictOptions=False,
        )
        with Session() as session:
            success = session.run(runtime_options)
            company_name: str | None = None
            company_number: str | None = None
            document_date: datetime.datetime | None = None
            model_xbrls = session.get_models()
            if model_xbrls:
                model_xbrl = model_xbrls[0]
                company_name = self._get_value_by_local_name(model_xbrl, 'EntityCurrentLegalOrRegisteredName')
                company_number = self._get_value_by_local_name(model_xbrl, 'UKCompaniesHouseRegisteredNumber')
                document_date = cast(datetime.datetime, self._get_value_by_local_name(model_xbrl, 'BalanceSheetDate'))
            logs = session.get_logs('text', clear_logs=True)
            return IxbrlViewerResult(
                success=success,
                logs=logs,
                company_name=company_name,
                company_number=company_number,
                document_date=document_date,
            )
