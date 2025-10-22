// Import {registerRoutes} from './AdminPanel/AdminPanel.routes';
import {registerJContentRoutes} from './AdminPanel/JContent.routes';
import i18next from 'i18next';
import en from '../main/resources/javascript/locales/en.json';
import fr from '../main/resources/javascript/locales/fr.json';
import de from '../main/resources/javascript/locales/de.json';
import es from '../main/resources/javascript/locales/es.json';
import it from '../main/resources/javascript/locales/it.json';
import pt from '../main/resources/javascript/locales/pt.json';

const registerResources = () => {
    const bundles = [
        ['en', en],
        ['fr', fr],
        ['de', de],
        ['es', es],
        ['it', it],
        ['pt', pt]
    ];

    bundles.forEach(([lang, resource]) => {
        // Register 'contentReportReact' namespace
        const namespaceData = resource.contentReportReact;
        if (namespaceData && !i18next.hasResourceBundle(lang, 'contentReportReact')) {
            i18next.addResourceBundle(lang, 'contentReportReact', namespaceData, true, true);
        }

        // Register 'content-reports' namespace for jContent navigation labels
        const contentReportsData = resource.contentReportReact;
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
