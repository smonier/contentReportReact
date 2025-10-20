export const OVERVIEW_QUERY = `
    query ContentReportsOverview($siteKey: String!, $language: String) {
        admin {
            contentReports(siteKey: $siteKey, language: $language) {
                overview {
                    siteName
                    siteDisplayableName
                    nbPages
                    nbTemplates
                    nbUsers
                    nbContents
                    nbEditorialContents
                    nbWorkflowTasks
                    nbFiles
                    nbImages
                    languages
                    nbLanguages
                }
            }
        }
    }
`;

export const RAW_REPORT_QUERY = `
    query ContentReportRaw($siteKey: String!, $language: String, $reportId: String!, $parameterName1: String, $parameterValue1: String, $parameterName2: String, $parameterValue2: String, $parameterName3: String, $parameterValue3: String, $parameterName4: String, $parameterValue4: String, $parameterName5: String, $parameterValue5: String, $parameterName6: String, $parameterValue6: String, $offset: Int, $limit: Int, $sortColumn: Int, $sortDirection: String) {
        admin {
            contentReports(siteKey: $siteKey, language: $language) {
                rawReport(reportId: $reportId, parameterName1: $parameterName1, parameterValue1: $parameterValue1, parameterName2: $parameterName2, parameterValue2: $parameterValue2, parameterName3: $parameterName3, parameterValue3: $parameterValue3, parameterName4: $parameterName4, parameterValue4: $parameterValue4, parameterName5: $parameterName5, parameterValue5: $parameterValue5, parameterName6: $parameterName6, parameterValue6: $parameterValue6, offset: $offset, limit: $limit, sortColumn: $sortColumn, sortDirection: $sortDirection)
            }
        }
    }
`;

export const GET_ALL_USERS_QUERY = `
  query GetAllUsers {
    jcr {
      nodesByQuery(
        query: "SELECT * FROM [jnt:user]"
        queryLanguage: SQL2
      ) {
        nodes {
          name
          property(name: "j:email") {
            value
          }
        }
      }
    }
  }
`;

export const GET_SITE_LANGUAGES_QUERY = `
  query GetSiteLanguages($siteKey: String!) {
    jcr {
      nodeByPath(path: $siteKey) {
        site {
          languages {
            displayName
            language
          }
        }
      }
    }
  }
`;
