package com.frc.codex.discovery.fca;

import java.util.Date;
import java.util.List;

public interface FcaClient {
	List<FcaFiling> fetchAllSinceDate(Date sinceDate);
}
