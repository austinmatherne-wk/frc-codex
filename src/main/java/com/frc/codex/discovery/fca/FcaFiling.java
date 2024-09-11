package com.frc.codex.discovery.fca;

import java.time.LocalDateTime;

public record FcaFiling(
		String companyName,
		String downloadUrl,
		String filename,
		String infoUrl,
		String lei,
		String sequenceId,
		LocalDateTime submittedDate
) {
}
