package com.frc.codex.clients.fca;

import java.time.LocalDateTime;
import java.util.List;

public interface FcaClient {
	List<FcaFiling> fetchAllSinceDate(LocalDateTime sinceDate);
}
