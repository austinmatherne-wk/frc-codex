package com.frc.codex.clients.fca;

import java.time.LocalDateTime;

public record FcaFiling(
		String companyName,
		LocalDateTime documentDate,
		String downloadUrl,
		String filename,
		String infoUrl,
		String lei,
		String sequenceId,
		LocalDateTime submittedDate
) {
}
