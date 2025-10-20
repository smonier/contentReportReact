package org.jahia.modules.contentreports.graphql;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

/**
 * Exposes this bundle's @GraphQLTypeExtension classes to the DX GraphQL provider.
 * Marker component; no explicit implementation required.
 */
@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class ContentReportsGraphQLExtensionsProvider implements DXGraphQLExtensionsProvider {
    // Marker component - GraphQL extensions are auto-discovered
}
