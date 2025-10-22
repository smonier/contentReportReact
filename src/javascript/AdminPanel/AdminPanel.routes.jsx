import {registry} from '@jahia/ui-extender';
import {DEFAULT_ROUTE} from './AdminPanel.constants';
import AdminPanel from './AdminPanel';
import React, {Suspense} from 'react';

export const registerRoutes = () => {
    registry.add('adminRoute', 'contentReportReact', {
        targets: ['jcontent:1'],
        icon: window.jahia.moonstone.toIconComponent('Pie'),
        label: 'contentReportReact:label',
        path: `${DEFAULT_ROUTE}*`, // Catch everything and let the app handle routing logic
        defaultPath: DEFAULT_ROUTE,
        isSelectable: true,
        render: v => <Suspense fallback="loading ..."><AdminPanel match={v.match}/></Suspense>
    });

    console.debug('%c contentReportReact is activated', 'color: #3c8cba');
};
