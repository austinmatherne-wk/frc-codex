package com.frc.codex.discovery.fca;

import java.util.Date;

public record FcaFiling(
		String filename,
		String downloadUrl,
		String infoUrl,
		String sequenceId,
		Date submittedDate
) {
}
