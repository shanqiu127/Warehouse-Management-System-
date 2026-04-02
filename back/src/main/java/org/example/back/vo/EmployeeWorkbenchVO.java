package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmployeeWorkbenchVO {

    private SummaryInfo summary;
    private ProfileInfo profile;
    private DeptContactInfo deptContact;
    private List<String> tips;

    @Data
    public static class SummaryInfo {
        private Long userId;
        private String username;
        private String realName;
        private Long deptId;
        private String deptCode;
        private String deptName;
        private LocalDateTime currentLoginTime;
        private LocalDateTime lastLoginTime;
        private Long lowStockCount;
        private Long zeroStockCount;
    }

    @Data
    public static class ProfileInfo {
        private String empCode;
        private String position;
        private String phone;
        private String email;
    }

    @Data
    public static class DeptContactInfo {
        private String deptName;
        private String leader;
        private String phone;
    }
}
