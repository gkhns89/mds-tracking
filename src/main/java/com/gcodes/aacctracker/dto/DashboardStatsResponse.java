package com.gcodes.aacctracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalUsers;
    private long totalCompanies;
    private long activeUsers;
    private long activeCompanies;
    private long myCompanies;
    private long managedUsers;
    private String userRole;
    private String mostRecentActivity;
}