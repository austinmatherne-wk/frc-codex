import logging
import zipfile
from pathlib import Path

from arelle.RuntimeOptions import RuntimeOptions  # type: ignore
from arelle.api.Session import Session  # type: ignore

from processor.base.queue_manager import JobMessage
from processor.base.worker import Worker, WorkerResult

VIEWER_HTML_FILENAME = 'ixbrlviewer.html'


logger = logging.getLogger(__name__)


class IxbrlViewerWorker(Worker):

    def work(self, job_message: JobMessage, target_path: Path, viewer_directory: Path) -> WorkerResult:
        packages = []
        for parent in target_path.parents:
            if zipfile.is_zipfile(parent):
                packages.append(parent)
                break
        success, viewer_logs = self._generate_viewer(target_path, viewer_directory, packages)
        if not success:
            return WorkerResult(
                error='Viewer generation failed within Arelle. Check the logs for details.',
                logs=viewer_logs
            )
        viewer_path = viewer_directory / VIEWER_HTML_FILENAME
        if not viewer_path.exists():
            return WorkerResult(
                error='Arelle reported success but viewer was not found. Check the logs for details.',
                logs=viewer_logs
            )
        return WorkerResult(success=True, viewer_entrypoint=VIEWER_HTML_FILENAME, logs=viewer_logs)

    def _generate_viewer(self, target_path: Path, viewer_directory: Path, packages: list[Path]) -> tuple[bool, str]:
        runtime_options = RuntimeOptions(
            cacheDirectory='./_HTTP_CACHE',
            disablePersistentConfig=True,
            entrypointFile=str(target_path),
            # TODO: Enable this when we have taxonomy packages provided
            # internetConnectivity='offline',
            keepOpen=True,
            logFormat="[%(messageCode)s] %(message)s - %(file)s",
            logFile='logToBuffer',
            pluginOptions={
                'saveViewerDest': str(viewer_directory),
                'useStubViewer': True,
                'viewerNoCopyScript': True,
                'viewerURL': '/ixbrlviewer.js',
            },
            plugins='ixbrl-viewer',
            strictOptions=False,
            packages=[str(package) for package in packages],
        )
        with Session() as session:
            success = session.run(runtime_options)
            logs = session.get_logs('text', clear_logs=True)
            return success, logs
