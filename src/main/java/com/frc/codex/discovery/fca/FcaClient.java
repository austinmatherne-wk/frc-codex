package com.frc.codex.discovery.fca;

import java.time.LocalDateTime;
import java.util.List;

public interface FcaClient {
	List<FcaFiling> fetchAllSinceDate(LocalDateTime sinceDate);
}
