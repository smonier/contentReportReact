package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.lang3.StringUtils;
import org.jahia.exceptions.JahiaException;
import org.jahia.modules.contentreports.bean.BaseReport;
import org.jahia.modules.contentreports.bean.ReportOverview;
import org.jahia.modules.contentreports.exception.ContentReportException;
import org.jahia.modules.contentreports.service.ContentReportFactory;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.utils.LanguageCodeConverters;
import org.json.JSONException;

import javax.jcr.RepositoryException;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;


@GraphQLDescription("Collection of content reports available for a site")
public class GqlContentReports {

    private final JCRSessionWrapper session;
    private final JCRSiteNode siteNode;
    private final Locale locale;

    public GqlContentReports(String siteKey, String language) throws RepositoryException {
        this.session = JCRSessionFactory.getInstance().getCurrentUserSession();
        this.siteNode = (JCRSiteNode) session.getNode("/sites/" + siteKey);
        this.locale = resolveLocale(language, siteNode);
    }

    private Locale resolveLocale(String language, JCRSiteNode site) {
        Locale resolved = null;
        if (StringUtils.isNotBlank(language)) {
            resolved = LanguageCodeConverters.languageCodeToLocale(language);
        }
        if (resolved == null && StringUtils.isNotBlank(site.getDefaultLanguage())) {
            resolved = LanguageCodeConverters.languageCodeToLocale(site.getDefaultLanguage());
        }
        if (resolved == null) {
            resolved = site.getLanguagesAsLocales().isEmpty() ? Locale.ENGLISH : site.getLanguagesAsLocales().get(0);
        }
        return resolved;
    }

    @GraphQLField
    @GraphQLDescription("Summary report of pages, templates and users")
    public GqlReportOverview overview() throws RepositoryException, JSONException, JahiaException {
        ReportOverview report = new ReportOverview(siteNode);
        populateReport(report, null, null);
        return new GqlReportOverview(report.getJson());
    }

    @GraphQLField
    @GraphQLDescription("Execute a legacy content report by its identifier and parameters")
    public String rawReport(@GraphQLName("reportId")  String reportId,
                            @GraphQLName("parameterName1") String parameterName1,
                            @GraphQLName("parameterValue1") String parameterValue1,
                            @GraphQLName("parameterName2") String parameterName2,
                            @GraphQLName("parameterValue2") String parameterValue2,
                            @GraphQLName("parameterName3") String parameterName3,
                            @GraphQLName("parameterValue3") String parameterValue3,
                            @GraphQLName("parameterName4") String parameterName4,
                            @GraphQLName("parameterValue4") String parameterValue4,
                            @GraphQLName("parameterName5") String parameterName5,
                            @GraphQLName("parameterValue5") String parameterValue5,
                            @GraphQLName("parameterName6") String parameterName6,
                            @GraphQLName("parameterValue6") String parameterValue6,
                            @GraphQLName("offset") Integer offset,
                            @GraphQLName("limit") Integer limit,
                            @GraphQLName("sortColumn") Integer sortColumn,
                            @GraphQLName("sortDirection") String sortDirection)
            throws RepositoryException, JSONException, JahiaException, ContentReportException {
        // Build parameter map from individual name/value pairs
        // Allow empty string values as they may be intentionally passed (e.g., for includeIfEmpty fields)
        Map<String, String> paramMap = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(parameterName1)) {
            paramMap.put(parameterName1, parameterValue1 != null ? parameterValue1 : "");
        }
        if (StringUtils.isNotBlank(parameterName2)) {
            paramMap.put(parameterName2, parameterValue2 != null ? parameterValue2 : "");
        }
        if (StringUtils.isNotBlank(parameterName3)) {
            paramMap.put(parameterName3, parameterValue3 != null ? parameterValue3 : "");
        }
        if (StringUtils.isNotBlank(parameterName4)) {
            paramMap.put(parameterName4, parameterValue4 != null ? parameterValue4 : "");
        }
        if (StringUtils.isNotBlank(parameterName5)) {
            paramMap.put(parameterName5, parameterValue5 != null ? parameterValue5 : "");
        }
        if (StringUtils.isNotBlank(parameterName6)) {
            paramMap.put(parameterName6, parameterValue6 != null ? parameterValue6 : "");
        }

        BaseReport report = ContentReportFactory.build(reportId, siteNode, paramMap, sortColumn, sortDirection);
        populateReport(report, offset, limit);
        return report.getJson().toString();
    }

    private void populateReport(BaseReport report, Integer offset, Integer limit) throws RepositoryException, JSONException, JahiaException {
        int start = offset != null ? offset : 0;
        int size = limit != null ? limit : 10;
        report.setLocale(locale);
        report.execute(session, start, size);
    }
}
