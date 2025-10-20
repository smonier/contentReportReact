/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.contentreports.service;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Short description of the class
 *
 * @author nonico
 */
public class ExpiredConditionService implements ConditionService {

    @Override public Map<String, String> getConditions(JCRNodeWrapper node) throws RepositoryException {
        JCRNodeWrapper conditionVisibilityNode = node.getNode("j:conditionalVisibility");
        if (conditionVisibilityNode == null) {
            return Collections.emptyMap();
        }
        if (!conditionVisibilityNode.getNodeTypes().contains(Constants.JAHIANT_CONDITIONAL_VISIBILITY)) {
            return Collections.emptyMap();
        }

        Map<String, LocalDateTime> conditionsMap = new HashMap<>(Collections.emptyMap());
        for (JCRNodeWrapper childNode : conditionVisibilityNode.getNodes()) {
            for (String nodeType : childNode.getNodeTypes()) {
                if (nodeType.equals("jnt:startEndDateCondition")) {
                    LocalDateTime endDateTime = LocalDateTime.parse(childNode.getPropertyAsString("end"), DateTimeFormatter.ISO_DATE_TIME);
                    if (endDateTime.isBefore(LocalDateTime.now())) {
                        conditionsMap.put(childNode.getName(), endDateTime);
                    }
                } else {
                    return Collections.emptyMap();
                }
            }
        }
        return getLatestDateTime(conditionsMap);
    }

    public Map<String, String> getLatestDateTime(Map<String, LocalDateTime> conditions) {
        if (!conditions.isEmpty()) {
            Map.Entry<String, LocalDateTime> firstEntrySet = conditions.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .iterator()
                    .next();
            if (firstEntrySet.getKey() != null || firstEntrySet.getValue() != null) {
                return Collections.singletonMap(firstEntrySet.getKey(),
                        firstEntrySet.getValue().format(DateTimeFormatter.ofPattern("MM/dd/yyy HH:mm")));
            }
        }
        return Collections.emptyMap();
    }


}
