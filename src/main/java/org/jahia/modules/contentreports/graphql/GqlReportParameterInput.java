package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLType;

/**
 * GraphQL input type for passing parameters to legacy content reports.
 */
@GraphQLType
@GraphQLName("ContentReportParameterInput")
@GraphQLDescription("Key/value pair to pass additional parameters to legacy content reports")
public class GqlReportParameterInput {

    private String name;
    private String value;

    public GqlReportParameterInput() {
    }

    public GqlReportParameterInput(@GraphQLName("name") String name,
                                   @GraphQLName("value") String value) {
        this.name = name;
        this.value = value;
    }

    @GraphQLField
    @GraphQLName("name")
    @GraphQLDescription("Parameter name expected by the legacy report")
    public String getName() {
        return name;
    }

    @GraphQLField
    @GraphQLName("value")
    @GraphQLDescription("Parameter value to forward to the legacy report")
    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
