package com.qvety.platform;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, String> {

    String TRIAL_DAYS = "trial_days";
    String CLOSED_RETENTION_DAYS = "closed_retention_days";
}
