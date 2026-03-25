package org.example.back.service;

import org.example.back.entity.SysIpPolicy;
import org.example.back.mapper.SysIpPolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.List;

@Service
public class IpPolicyService {

    @Autowired
    private SysIpPolicyMapper sysIpPolicyMapper;

    public IpCheckResult checkIp(String clientIp) {
        List<SysIpPolicy> policies = sysIpPolicyMapper.selectEnabledPolicies();
        if (policies == null || policies.isEmpty()) {
            return IpCheckResult.allow("NO_POLICY", "", clientIp);
        }

        for (SysIpPolicy policy : policies) {
            String rule = policy.getIpCidr();
            if (!matches(clientIp, rule)) {
                continue;
            }
            boolean allowed = Integer.valueOf(1).equals(policy.getAllowFlag());
            if (allowed) {
                return IpCheckResult.allow(policy.getPolicyName(), rule, clientIp);
            }
            return IpCheckResult.deny(policy.getPolicyName(), rule, clientIp);
        }

        // 已配置策略时，未命中任何允许规则则拒绝（白名单模式）
        return IpCheckResult.deny("NO_MATCH_DENY", "", clientIp);
    }

    private boolean matches(String clientIp, String rule) {
        if (clientIp == null || clientIp.isBlank() || rule == null || rule.isBlank()) {
            return false;
        }
        String normalizedIp = normalizeIp(clientIp);
        String normalizedRule = rule.trim();
        if (normalizedRule.contains("/")) {
            return matchCidr(normalizedIp, normalizedRule);
        }
        return normalizedIp.equals(normalizeIp(normalizedRule));
    }

    private String normalizeIp(String ip) {
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.trim();
    }

    private boolean matchCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            InetAddress ipAddr = InetAddress.getByName(ip);
            InetAddress networkAddr = InetAddress.getByName(parts[0]);
            int prefix = Integer.parseInt(parts[1]);

            byte[] ipBytes = ipAddr.getAddress();
            byte[] networkBytes = networkAddr.getAddress();
            if (ipBytes.length != networkBytes.length) {
                return false;
            }

            int maxPrefix = ipBytes.length * 8;
            if (prefix < 0 || prefix > maxPrefix) {
                return false;
            }

            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (8 - remainingBits);
            return (ipBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        } catch (Exception ex) {
            return false;
        }
    }

    public static class IpCheckResult {
        private final boolean allowed;
        private final String policyName;
        private final String matchedRule;
        private final String clientIp;

        private IpCheckResult(boolean allowed, String policyName, String matchedRule, String clientIp) {
            this.allowed = allowed;
            this.policyName = policyName;
            this.matchedRule = matchedRule;
            this.clientIp = clientIp;
        }

        public static IpCheckResult allow(String policyName, String matchedRule, String clientIp) {
            return new IpCheckResult(true, policyName, matchedRule, clientIp);
        }

        public static IpCheckResult deny(String policyName, String matchedRule, String clientIp) {
            return new IpCheckResult(false, policyName, matchedRule, clientIp);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getPolicyName() {
            return policyName;
        }

        public String getMatchedRule() {
            return matchedRule;
        }

        public String getClientIp() {
            return clientIp;
        }
    }
}
