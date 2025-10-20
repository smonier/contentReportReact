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

import org.jahia.services.content.JCRNodeWrapper;

import javax.jcr.RepositoryException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Short description of the class
 *
 * @author nonico
 */
public class FutureConditionService implements ConditionService {
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyy HH:mm");
    private final String CONDITIONALVISIBILITY_NT = "jnt:conditionalVisibility";
    private final String STARTENDDATECONDITION_NT = "jnt:startEndDateCondition";
    private final String CONDITIONALVISIBILITY_PROP = "j:conditionalVisibility";

    @Override public Map<String, String> getConditions(JCRNodeWrapper node) throws RepositoryException {
        JCRNodeWrapper conditionalVisibilityNode = node.getNode(CONDITIONALVISIBILITY_PROP);

        if (conditionalVisibilityNode == null) {
            return Collections.emptyMap();
        }

        if (!conditionalVisibilityNode.getNodeTypes().contains(CONDITIONALVISIBILITY_NT)) {
            return Collections.emptyMap();
        }

        Map<String, LocalDateTime> conditionsMap = new HashMap<>(Collections.emptyMap());
        for (JCRNodeWrapper childNode : conditionalVisibilityNode.getNodes()) {
            for (String nodeType : childNode.getNodeTypes()) {
                if (STARTENDDATECONDITION_NT.equals(nodeType)) {
                    LocalDateTime startDateTime = LocalDateTime.parse(childNode.getPropertyAsString("start"),
                            DateTimeFormatter.ISO_DATE_TIME);
                    if (startDateTime.isAfter(LocalDateTime.now())) {
                        conditionsMap.put(childNode.getName(), startDateTime);
                    }
                } else {
                    return Collections.emptyMap();
                }
            }
        }
        return getEarliestStartDate(conditionsMap);
    }

    /**
     * Determine the earliest start date
     * @param startDates
     * @return
     */
    public Map<String, String> getEarliestStartDate(Map<String, LocalDateTime> startDates) {
        if (!startDates.isEmpty()) {
            Map.Entry<String, LocalDateTime> firstEntrySet = startDates.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .iterator()
                    .next();
            if (firstEntrySet.getKey() != null || firstEntrySet.getValue() != null) {
                return Collections.singletonMap(firstEntrySet.getKey(), firstEntrySet.getValue().format(dateTimeFormatter));
            }
        }
        return Collections.emptyMap();
    }
}
