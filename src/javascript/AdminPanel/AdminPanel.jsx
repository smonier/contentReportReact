import React, {useCallback, useEffect, useMemo, useState} from 'react';
import PropTypes from 'prop-types';
import {Header, LayoutContent, Accordion, AccordionItem, Paper, Typography, Button, Input} from '@jahia/moonstone';
import {useTranslation} from 'react-i18next';
import axios from 'axios';
import {buildReportsConfig, reportCategories} from './AdminPanel.constants';
import ByAuthorAndDate from './reports/ByAuthorAndDate';
import ReportResultsTable from './reports/ReportResultsTable';
import styles from './AdminPanel.module.scss';
import {OVERVIEW_QUERY, RAW_REPORT_QUERY, GET_SITE_LANGUAGES_QUERY} from '../graphql/queries';

const REPORT_COMPONENTS = {
    ByAuthorAndDate
};

const resolveReportComponent = componentName => {
    if (!componentName) {
        return null;
    }

    // Direct mapping approach - try exact match first
    if (componentName === 'ByAuthorAndDate') {
        return ByAuthorAndDate;
    }

    // Fallback to dynamic lookup
    const normalizedName = typeof componentName === 'string' ? componentName.trim() : String(componentName);
    const componentEntry = REPORT_COMPONENTS[normalizedName];

    if (!componentEntry) {
        return null;
    }

    // Handle ES modules with default export
    if (typeof componentEntry === 'object' && componentEntry !== null && 'default' in componentEntry) {
        return componentEntry.default;
    }

    return componentEntry;
};

const overviewStyles = {
    grid: {display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginTop: '16px'},
    card: {padding: '16px', borderRadius: '6px', background: '#ffffff', border: '1px solid #e1e7f5', boxShadow: '0 1px 2px rgba(0,0,0,0.05)'},
    value: {fontSize: '26px', fontWeight: '700', color: '#0c2f6b', marginTop: '6px'}
};

const buildInitialFields = (report, context) => {
    if (report && report.fields) {
        return report.fields.reduce((acc, field) => {
            const defaultValue = typeof field.defaultValue === 'function' ? field.defaultValue(context) : field.defaultValue;
            if (field.type === 'checkbox') {
                acc[field.name] = defaultValue === undefined ? false : Boolean(defaultValue);
            } else {
                acc[field.name] = defaultValue === undefined ? '' : defaultValue;
            }

            return acc;
        }, {});
    }

    return {};
};

const isFieldActive = (field, values) => {
    if (field.dependsOn) {
        const dependencyValue = values[field.dependsOn];
        if (field.dependsValue !== undefined) {
            return dependencyValue === field.dependsValue;
        }

        return Boolean(dependencyValue);
    }

    return true;
};

const buildParameterPayload = (fields, definitions) => {
    const entries = [];

    console.log('[buildParameterPayload] Input fields:', fields);
    console.log('[buildParameterPayload] Definitions:', definitions);

    (definitions || []).forEach(field => {
        if (!isFieldActive(field, fields)) {
            console.log(`[buildParameterPayload] Field ${field.name} is not active, skipping`);
            return;
        }

        const value = fields[field.name];
        console.log(`[buildParameterPayload] Processing field ${field.name}, value:`, value, 'type:', field.type);

        if (field.type === 'checkbox') {
            entries.push({name: field.name, value: value ? 'true' : 'false'});
            console.log(`[buildParameterPayload] Added checkbox ${field.name} = ${value ? 'true' : 'false'}`);
            return;
        }

        if (value === undefined || value === null || `${value}`.trim() === '') {
            if (field.includeIfEmpty) {
                entries.push({name: field.name, value: ''});
                console.log(`[buildParameterPayload] Added empty field ${field.name} (includeIfEmpty=true)`);
            } else {
                console.log(`[buildParameterPayload] Skipping empty field ${field.name} (includeIfEmpty=false)`);
            }

            return;
        }

        entries.push({name: field.name, value: `${value}`});
        console.log(`[buildParameterPayload] Added field ${field.name} = ${value}`);
    });

    console.log('[buildParameterPayload] Final entries:', entries);
    return entries;
};

const validateFields = (definitions, values, t) => {
    for (const field of definitions || []) {
        if (!isFieldActive(field, values)) {
            continue;
        }

        const required = field.required || (typeof field.requiredWhen === 'function' && field.requiredWhen(values));
        if (!required) {
            continue;
        }

        if (field.type === 'checkbox') {
            continue;
        }

        const value = values[field.name];
        if (value === undefined || value === null || `${value}`.trim() === '') {
            return t('errors.requiredField', {field: t(field.labelKey)});
        }
    }

    return null;
};

const tryParseJSON = value => {
    if (typeof value === 'string') {
        try {
            const parsed = JSON.parse(value);

            // Normalize different backend response formats
            // Some reports return {totalContent, items: [...]} instead of {recordsTotal, data: [...]}
            if (parsed && parsed.items && Array.isArray(parsed.items)) {
                // Convert items array of objects to array of arrays for table rendering
                const data = parsed.items.map(item => {
                    // Extract values in a consistent order based on keys
                    return [item.title, item.type, item.path, item.date];
                });

                return {
                    recordsTotal: parsed.totalContent || data.length,
                    recordsFiltered: parsed.totalContent || data.length,
                    data: data
                };
            }

            return parsed;
        } catch (_) {
            return null;
        }
    }

    return value;
};

const OverviewResult = ({data, labelKey, descriptionKey, t}) => {
    if (!data) {
        return null;
    }

    const metrics = [
        {label: t('result.nbPages'), value: data.nbPages},
        {label: t('result.nbTemplates'), value: data.nbTemplates},
        {label: t('result.nbUsers'), value: data.nbUsers},
        {label: t('result.nbContents'), value: data.nbContents},
        {label: t('result.nbEditorialContents'), value: data.nbEditorialContents},
        {label: t('result.nbWorkflowTasks'), value: data.nbWorkflowTasks},
        {label: t('result.nbFiles'), value: data.nbFiles},
        {label: t('result.nbImages'), value: data.nbImages}
    ];

    // Function to get flag emoji from language code
    const getFlagEmoji = langCode => {
        const flagMap = {
            en: '🇬🇧',
            fr: '🇫🇷',
            de: '🇩🇪',
            es: '🇪🇸',
            it: '🇮🇹',
            pt: '🇵🇹',
            nl: '🇳🇱',
            ru: '🇷🇺',
            ja: '🇯🇵',
            zh: '🇨🇳',
            ar: '🇸🇦',
            ko: '🇰🇷',
            pl: '🇵🇱',
            tr: '🇹🇷',
            sv: '🇸🇪',
            da: '🇩🇰',
            no: '🇳🇴',
            fi: '🇫🇮',
            cs: '🇨🇿',
            el: '🇬🇷'
        };
        return flagMap[langCode] || '🌐';
    };

    return (
        <Paper style={{padding: '32px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04)'}}>
            <div style={{paddingBottom: '20px', borderBottom: '1px solid #e8ecf0', marginBottom: '24px'}}>
                <Typography variant="heading" weight="bold" style={{fontSize: '20px', color: '#1a2332', marginBottom: '6px', lineHeight: '1.4'}}>
                    {t(labelKey)}
                </Typography>
                {descriptionKey && (
                    <Typography variant="body" style={{color: '#5c6f90', fontSize: '14px', lineHeight: '1.6'}}>
                        {t(descriptionKey)}
                    </Typography>
                )}
            </div>
            <div>
                <div style={{color: '#2c3e5d', fontWeight: '600', fontSize: '16px', marginBottom: '16px'}}>{data.siteDisplayableName || data.siteName}</div>
                <div style={overviewStyles.grid}>
                    {metrics.map(item => (
                        <div key={item.label} style={overviewStyles.card}>
                            <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{item.label}</div>
                            <div style={overviewStyles.value}>{item.value}</div>
                        </div>
                    ))}
                    {data.languages && data.languages.length > 0 && (
                        <div style={{...overviewStyles.card, gridColumn: 'span 2'}}>
                            <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500', marginBottom: '8px'}}>
                                {t('result.languages')} ({data.nbLanguages})
                            </div>
                            <div style={{display: 'flex', flexWrap: 'wrap', gap: '8px'}}>
                                {data.languages.map(lang => (
                                    <div
                                        key={lang}
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '6px',
                                            padding: '6px 12px',
                                            backgroundColor: '#f4f6f9',
                                            borderRadius: '6px',
                                            fontSize: '14px',
                                            fontWeight: '500',
                                            color: '#2c3e5d'
                                        }}
                                    >
                                        <span style={{fontSize: '18px'}}>{getFlagEmoji(lang)}</span>
                                        <span>{lang.toUpperCase()}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </Paper>
    );
};

OverviewResult.propTypes = {
    data: PropTypes.shape({
        nbPages: PropTypes.number,
        nbTemplates: PropTypes.number,
        nbUsers: PropTypes.number,
        nbContents: PropTypes.number,
        nbEditorialContents: PropTypes.number,
        nbWorkflowTasks: PropTypes.number,
        nbFiles: PropTypes.number,
        nbImages: PropTypes.number,
        languages: PropTypes.arrayOf(PropTypes.string),
        nbLanguages: PropTypes.number,
        siteDisplayableName: PropTypes.string,
        siteName: PropTypes.string
    }),
    labelKey: PropTypes.string.isRequired,
    descriptionKey: PropTypes.string,
    t: PropTypes.func.isRequired
};

const RawResult = ({result, siteKey, language, labelKey, descriptionKey, selectedReport, t}) => {
    if (!result) {
        return null;
    }

    const parsed = result.parsed;

    return (
        <Paper style={{padding: '32px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04)'}}>
            <div style={{paddingBottom: '20px', borderBottom: '1px solid #e8ecf0', marginBottom: '24px'}}>
                <Typography variant="heading" weight="bold" style={{fontSize: '20px', color: '#1a2332', marginBottom: '6px', lineHeight: '1.4'}}>
                    {t(labelKey)}
                </Typography>
                {descriptionKey && (
                    <Typography variant="body" style={{color: '#5c6f90', fontSize: '14px', lineHeight: '1.6'}}>
                        {t(descriptionKey)}
                    </Typography>
                )}
            </div>
            {parsed && parsed.data && (
                <ReportResultsTable
                    data={parsed}
                    siteKey={siteKey}
                    language={language}
                    columns={selectedReport?.columns}
                    reportId={selectedReport?.id}
                    reportType={selectedReport?.type}
                />
            )}
        </Paper>
    );
};

RawResult.propTypes = {
    result: PropTypes.shape({
        payload: PropTypes.string,
        parsed: PropTypes.shape({
            recordsTotal: PropTypes.number,
            recordsFiltered: PropTypes.number,
            data: PropTypes.arrayOf(PropTypes.array)
        })
    }),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string,
    labelKey: PropTypes.string.isRequired,
    descriptionKey: PropTypes.string,
    selectedReport: PropTypes.object,
    t: PropTypes.func.isRequired
};

const ReportMenu = ({reports, selectedReportId, onSelectReport, t}) => {
    const [openedCategory, setOpenedCategory] = useState('overview');

    // Group reports by category
    const reportsByCategory = useMemo(() => {
        const grouped = {};
        reportCategories.forEach(cat => {
            grouped[cat.id] = [];
        });

        (reports || []).forEach(report => {
            if (report.category && grouped[report.category]) {
                grouped[report.category].push(report);
            }
        });

        return grouped;
    }, [reports]);

    return (
        <Paper className={styles.menuPaper}>
            <nav className={styles.menu}>
                <Accordion
                    openedItem={openedCategory}
                    onSetOpenedItem={setOpenedCategory}
                >
                    {reportCategories.map(category => {
                        const categoryReports = reportsByCategory[category.id];
                        if (!categoryReports || categoryReports.length === 0) {
                            return null;
                        }

                        return (
                            <AccordionItem
                                key={category.id}
                                id={category.id}
                                label={t(category.labelKey)}
                            >
                                <div className={styles.categoryReports}>
                                    {categoryReports.map(report => {
                                        const isActive = report.id === selectedReportId;
                                        const buttonClassName = `${styles.menuButton}${isActive ? ` ${styles.menuButtonActive}` : ''}`;

                                        return (
                                            <button
                                                key={report.id}
                                                type="button"
                                                className={buttonClassName}
                                                onClick={() => onSelectReport(report.id)}
                                            >
                                                {t(report.labelKey)}
                                            </button>
                                        );
                                    })}
                                </div>
                            </AccordionItem>
                        );
                    })}
                </Accordion>
            </nav>
        </Paper>
    );
};

ReportMenu.propTypes = {
    onSelectReport: PropTypes.func.isRequired,
    reports: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        labelKey: PropTypes.string.isRequired,
        category: PropTypes.string
    })),
    selectedReportId: PropTypes.string,
    t: PropTypes.func.isRequired
};

const FormActions = ({isLoading, lastUpdated, t}) => (
    <div className={styles.actions}>
        <button type="submit" className={styles.buttonPrimary} disabled={isLoading}>
            {isLoading ? t('states.loading') : t('actions.runReport')}
        </button>
        {lastUpdated && (
            <span style={{color: '#4b5c87', fontSize: '13px'}}>
                {t('states.lastUpdated', {value: lastUpdated.toLocaleString()})}
            </span>
        )}
    </div>
);

FormActions.propTypes = {
    isLoading: PropTypes.bool.isRequired,
    lastUpdated: PropTypes.instanceOf(Date),
    t: PropTypes.func.isRequired
};

const ResultSection = ({error, isLoading, result, siteKey, language, report, t}) => (
    <>
        {error && (
            <div className={styles.error}>
                {error.message}
            </div>
        )}

        {!isLoading && result && result.kind === 'overview' && (
            <OverviewResult
                data={result.overview}
                labelKey={report.labelKey}
                descriptionKey={report.descriptionKey}
                t={t}
            />
        )}

        {!isLoading && result && result.kind === 'legacy' && (
            <RawResult
                result={result}
                siteKey={siteKey}
                language={language}
                labelKey={report.labelKey}
                descriptionKey={report.descriptionKey}
                selectedReport={report}
                t={t}
            />
        )}

        {isLoading && (
            <div className={styles.info}>{t('states.loading')}</div>
        )}
    </>
);

ResultSection.propTypes = {
    error: PropTypes.instanceOf(Error),
    isLoading: PropTypes.bool.isRequired,
    result: PropTypes.shape({
        kind: PropTypes.string,
        overview: PropTypes.object
    }),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string,
    report: PropTypes.shape({
        labelKey: PropTypes.string.isRequired,
        descriptionKey: PropTypes.string
    }).isRequired,
    t: PropTypes.func.isRequired
};

const AdminPanel = () => {
    const {t} = useTranslation('contentReportReact');

    const context = window.contextJsParameters || {};
    const siteKey = context.siteKey || (context.site && context.site.key) || '';
    const language = context.lang || context.language || '';
    const baseContentPath = useMemo(() => (siteKey ? `/sites/${siteKey}` : ''), [siteKey]);
    const graphqlEndpoint = `${context.contextPath || ''}/modules/graphql`;

    const reports = useMemo(() => buildReportsConfig(), []);
    const [selectedReportId, setSelectedReportId] = useState(reports[0]?.id);

    const selectedReport = useMemo(
        () => reports.find(report => report.id === selectedReportId) || reports[0],
        [reports, selectedReportId]
    );

    const [fields, setFields] = useState(() => buildInitialFields(selectedReport, {siteKey, language}));
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [result, setResult] = useState(null);
    const [lastUpdated, setLastUpdated] = useState(null);
    const [siteLanguages, setSiteLanguages] = useState([]);

    // Fetch site languages on mount
    useEffect(() => {
        const fetchSiteLanguages = async () => {
            if (!siteKey) {
                return;
            }

            try {
                const data = await executeQuery(GET_SITE_LANGUAGES_QUERY, {siteKey: `/sites/${siteKey}`});
                const languagesData = data?.jcr?.nodeByPath?.site?.languages || [];
                const languages = languagesData.map(lang => lang.language);
                console.log('Fetched site languages:', languages);
                setSiteLanguages(languages);
            } catch (err) {
                console.error('Failed to fetch site languages:', err);
                // Fallback: try to get from context
                const contextLanguages = window.contextJsParameters?.siteLanguages || [];
                console.log('Using fallback languages from context:', contextLanguages);
                setSiteLanguages(contextLanguages);
            }
        };

        fetchSiteLanguages();
    }, [siteKey, executeQuery]);

    useEffect(() => {
        setFields(buildInitialFields(selectedReport, {siteKey, language}));
        setResult(null);
        setError(null);
    }, [selectedReport, siteKey, language]);

    const handleFieldChange = useCallback((name, value) => {
        console.log('[AdminPanel] handleFieldChange - name:', name, 'value:', value);
        setFields(prev => {
            const newFields = {...prev, [name]: value};
            console.log('[AdminPanel] handleFieldChange - new fields state:', newFields);
            return newFields;
        });
    }, []);

    const executeQuery = useCallback(async (query, variables) => {
        const response = await axios.post(graphqlEndpoint, {query, variables}, {
            headers: {'Content-Type': 'application/json'},
            withCredentials: true
        });

        if (response.data && response.data.errors && response.data.errors.length) {
            const message = response.data.errors.map(err => err.message).join('\n');
            throw new Error(message);
        }

        return response.data.data;
    }, [graphqlEndpoint]);

    const runOverview = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await executeQuery(OVERVIEW_QUERY, {siteKey, language});
            const overview = data?.admin?.contentReports?.overview;
            setResult({kind: 'overview', overview});
            setLastUpdated(new Date());
        } catch (err) {
            setError(err);
        } finally {
            setLoading(false);
        }
    }, [executeQuery, siteKey, language]);

    const runLegacy = useCallback(async () => {
        if (!selectedReport) {
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const parameters = buildParameterPayload(fields, selectedReport.fields);

            console.log('[AdminPanel] runLegacy - fields:', fields);
            console.log('[AdminPanel] runLegacy - parameters:', parameters);

            // Convert parameters array to individual name/value pairs (max 5 parameters supported)
            const variables = {
                siteKey,
                language,
                reportId: selectedReport.id,
                offset: selectedReport.defaultOffset ?? 0,
                limit: selectedReport.defaultLimit ?? 10000, // Fetch all results for client-side pagination
                sortColumn: selectedReport.defaultSortColumn ?? null,
                sortDirection: selectedReport.defaultSortDirection ?? null
            };

            // Add up to 6 parameters as individual fields
            parameters.forEach((param, index) => {
                if (index < 6) {
                    variables[`parameterName${index + 1}`] = param.name;
                    variables[`parameterValue${index + 1}`] = param.value;
                }
            });

            console.log('[AdminPanel] runLegacy - GraphQL variables:', variables);

            const data = await executeQuery(RAW_REPORT_QUERY, variables);
            const payload = data?.admin?.contentReports?.rawReport;
            const parsed = tryParseJSON(payload);
            setResult({kind: 'legacy', payload, parsed});
            setLastUpdated(new Date());
        } catch (err) {
            setError(err);
        } finally {
            setLoading(false);
        }
    }, [executeQuery, fields, language, selectedReport, siteKey]);

    const handleSubmit = useCallback(event => {
        event.preventDefault();
        if (!selectedReport) {
            return;
        }

        const validationError = validateFields(selectedReport.fields, fields, t);
        if (validationError) {
            setError(new Error(validationError));
            return;
        }

        if (selectedReport.type === 'overview') {
            runOverview();
        } else {
            runLegacy();
        }
    }, [fields, runLegacy, runOverview, selectedReport, t]);

    useEffect(() => {
        if (selectedReport && selectedReport.type === 'overview') {
            runOverview();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedReportId]);

    const openPathPicker = useCallback((fieldName, currentValue) => {
        const initialPath = currentValue || baseContentPath || '/';
        const picker = window?.CE_API?.openPicker;

        console.log('[AdminPanel] openPathPicker - fieldName:', fieldName, 'currentValue:', currentValue, 'initialPath:', initialPath);

        if (picker) {
            picker({
                type: 'editorial',
                initialSelectedItem: [initialPath],
                site: window.jahiaGWTParameters?.siteKey || siteKey,
                lang: window.jahiaGWTParameters?.uilang || language || 'en',
                isMultiple: false,
                setValue: ([selected]) => {
                    console.log('[AdminPanel] Path picker returned:', selected);
                    if (selected?.path) {
                        console.log('[AdminPanel] Setting field', fieldName, 'to path:', selected.path);
                        handleFieldChange(fieldName, selected.path);
                    }
                }
            });
            return;
        }

        console.warn('[contentReportReact] Content Editor picker API is not available.');
    }, [baseContentPath, handleFieldChange, language, siteKey]);

    const renderGenericField = useCallback(field => {
        if (!field || !isFieldActive(field, fields)) {
            return null;
        }

        if (field.type === 'checkbox') {
            return (
                <label key={field.name} className={styles.checkboxRow}>
                    <input
                        type="checkbox"
                        checked={Boolean(fields[field.name])}
                        onChange={event => handleFieldChange(field.name, event.target.checked)}
                    />
                    <span>{t(field.labelKey)}</span>
                </label>
            );
        }

        if (field.type === 'radio') {
            return (
                <div key={field.name} className={styles.formRow}>
                    <span className={styles.label}>{t(field.labelKey)}</span>
                    {(field.options || []).map(option => (
                        <label key={`${field.name}-${option.value}`} className={styles.inline}>
                            <input
                                type="radio"
                                name={field.name}
                                value={option.value}
                                checked={fields[field.name] === option.value}
                                onChange={event => handleFieldChange(field.name, event.target.value)}
                            />
                            <span>{t(option.labelKey)}</span>
                        </label>
                    ))}
                </div>
            );
        }

        if (field.type === 'select' || field.type === 'languageSelect') {
            const options = field.type === 'languageSelect' ?
                siteLanguages.map(lang => ({value: lang, labelKey: lang})) :
                (field.options || []);

            return (
                <div key={field.name} className={styles.formRow}>
                    <label className={styles.label} htmlFor={field.name}>{t(field.labelKey)}</label>
                    <select
                        id={field.name}
                        value={fields[field.name] ?? ''}
                        className={styles.input}
                        onChange={event => handleFieldChange(field.name, event.target.value)}
                    >
                        {options.map(option => (
                            <option key={option.value} value={option.value}>
                                {field.type === 'languageSelect' ? option.value : t(option.labelKey)}
                            </option>
                        ))}
                    </select>
                </div>
            );
        }

        const isPathField = field.type === 'path';
        const inputType = field.type === 'date' ? 'date' : 'text';
        const placeholder = field.placeholderKey ? t(field.placeholderKey) : '';

        return (
            <div key={field.name} className={styles.formRow}>
                <label className={styles.label} htmlFor={field.name}>{t(field.labelKey)}</label>
                <div className={isPathField ? styles.inline : styles.inputRow}>
                    <Input
                        id={field.name}
                        type={inputType}
                        value={fields[field.name] ?? ''}
                        size="big"
                        placeholder={placeholder}
                        onChange={event => handleFieldChange(field.name, event.target.value)}
                    />
                    {isPathField && (
                        <Button
                            size="big"
                            variant="outlined"
                            label={t('actions.browse')}
                            onClick={() => openPathPicker(field.name, fields[field.name])}
                        />
                    )}
                </div>
            </div>
        );
    }, [fields, handleFieldChange, openPathPicker, t, siteLanguages]);

    const renderFormFields = () => {
        if (!selectedReport || selectedReport.type === 'overview') {
            return null;
        }

        // Direct component rendering for known components
        if (selectedReport.component === 'ByAuthorAndDate') {
            return (
                <ByAuthorAndDate
                    baseContentPath={baseContentPath}
                    definitions={selectedReport.fields || []}
                    fields={fields}
                    t={t}
                    onFieldChange={handleFieldChange}
                    onOpenPathPicker={openPathPicker}
                />
            );
        }

        // Dynamic component resolution for other cases
        if (selectedReport.component) {
            const ReportComponent = resolveReportComponent(selectedReport.component);

            if (ReportComponent && typeof ReportComponent === 'function') {
                return React.createElement(ReportComponent, {
                    baseContentPath,
                    definitions: selectedReport.fields || [],
                    fields,
                    t,
                    onFieldChange: handleFieldChange,
                    onOpenPathPicker: openPathPicker
                });
            }
        }

        // Fallback to generic fields
        const genericFields = selectedReport.fields?.map(renderGenericField) || null;
        return genericFields ? (
            <Paper style={{padding: '32px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04)', display: 'flex', flexDirection: 'column', gap: '28px'}}>
                <div style={{paddingBottom: '20px', borderBottom: '1px solid #e8ecf0'}}>
                    <Typography variant="heading" weight="bold" style={{fontSize: '20px', color: '#1a2332', marginBottom: '6px', lineHeight: '1.4'}}>
                        {t(selectedReport.labelKey)}
                    </Typography>
                    {selectedReport.descriptionKey && (
                        <Typography variant="body" style={{color: '#5c6f90', fontSize: '14px', lineHeight: '1.6'}}>
                            {t(selectedReport.descriptionKey)}
                        </Typography>
                    )}
                </div>
                {genericFields}
            </Paper>
        ) : null;
    };

    return (
        <>
            <Header
                title={`${t('label')} ${siteKey ? `- ${siteKey}` : ''}`}
            />
            <LayoutContent
                content={(
                    <div className={styles.pageContainer}>
                        <div className={styles.panelLayout}>
                            <ReportMenu
                                reports={reports}
                                selectedReportId={selectedReportId}
                                t={t}
                                onSelectReport={setSelectedReportId}
                            />
                            <div className={styles.content}>
                                {selectedReport?.type === 'overview' ? (
                                    // For overview report: show result panel on top, button below
                                    <>
                                        {!loading && result && result.kind === 'overview' && (
                                            <OverviewResult
                                                data={result.overview}
                                                labelKey={selectedReport.labelKey}
                                                descriptionKey={selectedReport.descriptionKey}
                                                t={t}
                                            />
                                        )}
                                        <form className={styles.form} onSubmit={handleSubmit}>
                                            <FormActions isLoading={loading} lastUpdated={lastUpdated} t={t}/>
                                        </form>
                                    </>
                                ) : selectedReport ? (
                                    <form className={styles.form} onSubmit={handleSubmit}>
                                        {renderFormFields()}
                                        <FormActions isLoading={loading} lastUpdated={lastUpdated} t={t}/>
                                    </form>
                                ) : (
                                    <div className={styles.error}>
                                        {t('errors.noReportSelected')}
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className={styles.fullWidthResults}>
                            {selectedReport?.type !== 'overview' && (
                                <ResultSection error={error} isLoading={loading} result={result} siteKey={siteKey} language={language} report={selectedReport} t={t}/>
                            )}
                        </div>
                    </div>
                )}
            />
        </>
    );
};

AdminPanel.displayName = 'ContentReportAdminPanel';

export default AdminPanel;
