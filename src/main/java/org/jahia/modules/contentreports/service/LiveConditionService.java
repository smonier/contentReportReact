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
import org.jahia.services.visibility.VisibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Short description of the class
 *
 * @author nonico
 */
public class LiveConditionService implements ConditionService {
    private static final Logger logger = LoggerFactory.getLogger(LiveConditionService.class);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    private static final String JAHIANT_DAY_OF_WEEK_CONDITION = "jnt:dayOfWeekCondition";
    private static final String JAHIANT_START_END_DATE_CONDITION = "jnt:startEndDateCondition";
    private static final String JAHIANT_TIME_OF_DAY_CONDITION = "jnt:timeOfDayCondition";
    private static final String CONDITIONAL_VISIBILITY_PROP = "j:conditionalVisibility";
    private static final String START_HOUR = "startHour";
    private static final String START_MINUTE = "startMinute";
    private static final String END_HOUR = "endHour";
    private static final String END_MINUTE = "endMinute";
    private static final String START_DATE_PROPERTY = "start";
    private static final String END_DATE_PROPERTY = "end";
    public static final String CURRENT_STATUS = "currentStatus";
    public static final String IS_CONDITION_MATCHED = "isConditionMatched";

    @Override public Map<String, String> getConditions(JCRNodeWrapper node) throws RepositoryException {
        JCRNodeWrapper conditionalVisibilityNode = node.getNode(CONDITIONAL_VISIBILITY_PROP);
        if (conditionalVisibilityNode == null ||
                !conditionalVisibilityNode.getNodeTypes().contains(Constants.JAHIANT_CONDITIONAL_VISIBILITY)) {
            return Collections.emptyMap();
        }
        Map<String, String> conditionsMap = new HashMap<>();
        boolean matchedAllConditions = true;
        for (JCRNodeWrapper childNode : conditionalVisibilityNode.getNodes()) {
            for (String nodeType : childNode.getNodeTypes()) {
                boolean isConditionMatched = false;
                if (nodeType.equals(JAHIANT_DAY_OF_WEEK_CONDITION)) {
                    conditionsMap.put(childNode.getName(), getDayOfWeekCondition(childNode));
                    isConditionMatched = checkDayOfWeek(childNode);
                } else if (nodeType.equals(JAHIANT_TIME_OF_DAY_CONDITION)) {
                    conditionsMap.put(childNode.getName(), getTimeOfDayCondition(childNode));
                    isConditionMatched = checkTimeOfDay(childNode);
                } else if (nodeType.equals(JAHIANT_START_END_DATE_CONDITION) && containsRecurringConditions(node)) {
                    conditionsMap.put(childNode.getName(), getStartEndDateCondition(childNode));
                    isConditionMatched = checkStartEndDate(childNode);
                }
                matchedAllConditions = isConditionMatched && matchedAllConditions;
            }
        }
        conditionsMap.put(CURRENT_STATUS, getCurrentStatus(node));
        conditionsMap.put(IS_CONDITION_MATCHED, String.valueOf(matchedAllConditions));
        return conditionsMap;
    }

    private boolean containsRecurringConditions(JCRNodeWrapper node) {
        Map<JCRNodeWrapper, Boolean> conditionMatchesDetails = VisibilityService.getInstance()
                .getConditionMatchesDetails(node);
        return VisibilityService.getInstance().matchesConditions(node) ||
                !conditionMatchesDetails.keySet().stream().allMatch(this::isStartEndDateConditionNode);
    }

    private String getCurrentStatus(JCRNodeWrapper node) throws RepositoryException {
        if (VisibilityService.getInstance().matchesConditions(node) &&
                node.hasProperty(Constants.PUBLISHED) &&
                node.getProperty(Constants.PUBLISHED).getBoolean()) {
            return "visible";
        }
        return "not visible";
    }

    private boolean isStartEndDateConditionNode(JCRNodeWrapper node) {
        try {
            return node.isNodeType(JAHIANT_START_END_DATE_CONDITION);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private String getTimeOfDayCondition(JCRNodeWrapper childNode) throws RepositoryException {
        String timeCondition = "Visible";
        String startHour = childNode.hasProperty(START_HOUR) ? childNode.getPropertyAsString(START_HOUR) : "";
        String startMinute = childNode.hasProperty(START_MINUTE) ? childNode.getPropertyAsString(START_MINUTE) : "00";
        String endHour = childNode.hasProperty(END_HOUR) ? childNode.getPropertyAsString(END_HOUR) : "";
        String endMinute = childNode.hasProperty(END_MINUTE) ? childNode.getPropertyAsString(END_MINUTE) : "00";
        if (startHour.isEmpty() && endHour.isEmpty()) {
            timeCondition += " any time of the day";
        } else if (startHour.isEmpty()) {
            timeCondition += String.format(" until %s:%s", endHour, endMinute);
        } else if (endHour.isEmpty()) {
            timeCondition += String.format(" from %s:%s", startHour, startMinute);
        } else {
            timeCondition += String.format(" from %s:%s until %s:%s",startHour, startMinute, endHour, endMinute);
        }
        return timeCondition;
    }

    private String getStartEndDateCondition(JCRNodeWrapper childNode) throws RepositoryException {
        StringBuilder dateConditionBuilder = new StringBuilder("Visible");
        String start = childNode.hasProperty(START_DATE_PROPERTY) ? childNode.getPropertyAsString(START_DATE_PROPERTY) : "";
        String end = childNode.hasProperty(END_DATE_PROPERTY) ? childNode.getPropertyAsString(END_DATE_PROPERTY) : "";
        LocalDateTime startDate;
        LocalDateTime endDate;
        if (!start.isEmpty()) {
            startDate = LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME);
            dateConditionBuilder.append(" starting from ").append(startDate.format(DATETIME_FORMAT));
        }
        if (!end.isEmpty()) {
            endDate = LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME);
            dateConditionBuilder.append(" until ").append(endDate.format(DATETIME_FORMAT));
        }
        return dateConditionBuilder.toString();
    }

    private String getDayOfWeekCondition(JCRNodeWrapper childNode) {
        String daysOfWeek = Arrays.stream(childNode.getPropertyAsString("dayOfWeek").split(" ").clone())
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .sorted()
                .map(Enum::toString)
                .map(String::toLowerCase)
                .collect(Collectors.joining(", "));
        return String.format("Visible on [ %s ]", daysOfWeek);
    }

    private boolean checkDayOfWeek(JCRNodeWrapper node) throws RepositoryException {
        if (node.getNodeTypes().size() != 1 || !node.getNodeTypes().contains(JAHIANT_DAY_OF_WEEK_CONDITION)) {
            return false;
        }
        return Arrays.stream(node.getPropertyAsString("dayOfWeek").split(" "))
                .anyMatch(day -> day.equalsIgnoreCase(LocalDate.now().getDayOfWeek().toString()));
    }

    private boolean checkStartEndDate(JCRNodeWrapper node) throws RepositoryException {
        if (node.getNodeTypes().size() != 1 || !node.getNodeTypes().contains(JAHIANT_START_END_DATE_CONDITION)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        String start = node.hasProperty(START_DATE_PROPERTY) ? node.getPropertyAsString(START_DATE_PROPERTY) : "";
        String end = node.hasProperty(END_DATE_PROPERTY) ? node.getPropertyAsString(END_DATE_PROPERTY) : "";
        if (start.isEmpty() && end.isEmpty()) {
            return false;
        } else if (start.isEmpty()) {
            return LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME).isAfter(now);
        } else if (end.isEmpty()) {
            return LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME).isBefore(now);
        } else {
            LocalDateTime startDate = LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime endDate = LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME);
            return startDate.isBefore(now) && endDate.isAfter(now);
        }
    }

    private boolean checkTimeOfDay(JCRNodeWrapper childNode) throws RepositoryException {
        LocalTime now = LocalTime.now();
        LocalTime startTime;
        LocalTime endTime;
        String startHour = childNode.hasProperty(START_HOUR) ? childNode.getPropertyAsString(START_HOUR) : "";
        String startMinute = childNode.hasProperty(START_MINUTE) ? childNode.getPropertyAsString(START_MINUTE) : "00";
        String endHour = childNode.hasProperty(END_HOUR) ? childNode.getPropertyAsString(END_HOUR) : "";
        String endMinute = childNode.hasProperty(END_MINUTE) ? childNode.getPropertyAsString(END_MINUTE) : "00";

        if (startHour.isEmpty() && endHour.isEmpty()) {
            return true;
        } else if (startHour.isEmpty()) {
            endTime = LocalTime.of(Integer.parseInt(endHour), Integer.parseInt(endMinute));
            return endTime.isAfter(now);
        } else if (endHour.isEmpty()) {
            startTime = LocalTime.of(Integer.parseInt(startHour), Integer.parseInt(startMinute));
            return startTime.isBefore(now);
        } else {
            startTime = LocalTime.of(Integer.parseInt(startHour), Integer.parseInt(startMinute));
            endTime = LocalTime.of(Integer.parseInt(endHour), Integer.parseInt(endMinute));
            return startTime.isBefore(now) && endTime.isAfter(now);
        }
    }
}
