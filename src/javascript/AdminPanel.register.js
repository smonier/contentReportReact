// Import {registerRoutes} from './AdminPanel/AdminPanel.routes';
import {registerJContentRoutes} from './AdminPanel/JContent.routes';
import i18next from 'i18next';
import en from '../main/resources/javascript/locales/en.json';
import fr from '../main/resources/javascript/locales/fr.json';

const registerResources = () => {
    const bundles = [
        ['en', en],
        ['fr', fr]
    ];

    bundles.forEach(([lang, resource]) => {
        const namespaceData = resource.contentReportReact || resource;
        if (namespaceData && !i18next.hasResourceBundle(lang, 'contentReportReact')) {
            i18next.addResourceBundle(lang, 'contentReportReact', namespaceData, true, true);
        }

        // Also register 'contentReportReact' namespace for jContent integration
        const contentReportsData = resource.contentReportReact || resource;
        if (contentReportsData && !i18next.hasResourceBundle(lang, 'contentReportReact')) {
            i18next.addResourceBundle(lang, 'contentReportReact', contentReportsData, true, true);
        }
    });
};

export default async function () {
    registerResources();
    await i18next.loadNamespaces('contentReportReact');
    // RegisterRoutes();
    registerJContentRoutes();
}
