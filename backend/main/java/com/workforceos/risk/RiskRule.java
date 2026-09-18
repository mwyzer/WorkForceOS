package com.workforceos.risk;

import java.util.List;

public interface RiskRule {

    RiskType type();

    List<RiskAssessment> detect(WorkforceRiskData data);
}