package org.example.back.scheduler;

import org.example.back.service.WorkRequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkRequirementOverdueScheduler {

    @Autowired
    private WorkRequirementService workRequirementService;

    @Scheduled(cron = "0 */10 * * * ?")
    public void scanOverdueAssignments() {
        workRequirementService.scanAndMarkOverdueAssignments();
    }
}