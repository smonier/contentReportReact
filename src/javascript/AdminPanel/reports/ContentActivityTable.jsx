import React, {useMemo, useState} from 'react';
import PropTypes from 'prop-types';
import {Collapsible, Table, TableHead, TableBody, TableRow, TableHeadCell, TableBodyCell, TablePagination, Button, CheckboxItem} from '@jahia/moonstone';
import {Download, OpenInNew} from '@jahia/moonstone/dist/icons';
import styles from './ContentActivityTable.module.scss';

const SORTABLE_COLUMNS = [
    {key: 'name', labelKey: 'result.activityTable.columns.name'},
    {key: 'path', labelKey: 'result.activityTable.columns.path'},
    {key: 'type', labelKey: 'result.activityTable.columns.type'},
    {key: 'created', labelKey: 'result.activityTable.columns.created'},
    {key: 'lastModified', labelKey: 'result.activityTable.columns.modified'},
    {key: 'lastPublished', labelKey: 'result.activityTable.columns.published'}
];

const EVENT_FILTERS = [
    {key: 'isNew', labelKey: 'result.activityTable.filters.created'},
    {key: 'isModified', labelKey: 'result.activityTable.filters.modified'},
    {key: 'isPublished', labelKey: 'result.activityTable.filters.published'}
];

const formatDateTime = value => {
    if (!value) {
        return null;
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
};

const buildJContentUrl = (path, siteKey, language) => {
    if (!path || !siteKey) {
        return null;
    }

    const baseUrl = window.contextJsParameters?.contextPath || '';
    let cleanPath = path.replace(`/sites/${siteKey}`, '');
    cleanPath = cleanPath.startsWith('/') ? cleanPath.substring(1) : cleanPath;

    return `${baseUrl}/jahia/jcontent/${siteKey}/${language || 'en'}/pages/${cleanPath}`;
};

const exportActivityToCSV = (rows, t) => {
    const headers = [
        t('result.activityTable.columns.name'),
        t('result.activityTable.columns.path'),
        t('result.activityTable.columns.type'),
        t('result.activityTable.columns.created'),
        t('result.activityTable.columns.createdBy'),
        t('result.activityTable.columns.modified'),
        t('result.activityTable.columns.modifiedBy'),
        t('result.activityTable.columns.published'),
        t('result.activityTable.columns.publishedBy')
    ];

    const escapeCell = value => {
        const cell = String(value === null || value === undefined ? '' : value);

        return cell.includes(',') || cell.includes('"') || cell.includes('\n') ?
            `"${cell.replace(/"/g, '""')}"` :
            cell;
    };

    const csvContent = [
        headers.map(escapeCell).join(','),
        ...rows.map(row => [
            row.name,
            row.path,
            row.type,
            row.created,
            row.createdBy,
            row.lastModified,
            row.lastModifiedBy,
            row.lastPublished,
            row.lastPublishedBy
        ].map(escapeCell).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], {type: 'text/csv;charset=utf-8;'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `content-activity_${new Date().getTime()}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
};

const compareRows = (left, right, sortColumn, sortDirection) => {
    const leftValue = left[sortColumn];
    const rightValue = right[sortColumn];

    // Rows without a value for the sorted column always sink to the bottom.
    if (!leftValue && !rightValue) {
        return 0;
    }

    if (!leftValue) {
        return 1;
    }

    if (!rightValue) {
        return -1;
    }

    let result;
    if (sortColumn === 'created' || sortColumn === 'lastModified' || sortColumn === 'lastPublished') {
        result = new Date(leftValue).getTime() - new Date(rightValue).getTime();
    } else {
        result = String(leftValue).localeCompare(String(rightValue));
    }

    return sortDirection === 'asc' ? result : -result;
};

// Returns the TableBodyCell element itself rather than a component, so that
// Moonstone's TableRow always sees real cells as its direct children.
const renderEventCell = (date, author, isHighlighted, unknownLabel) => {
    const formatted = formatDateTime(date);

    if (!formatted) {
        return (
            <TableBodyCell className={styles.eventCell}>
                <span className={styles.emptyValue}>-</span>
            </TableBodyCell>
        );
    }

    return (
        <TableBodyCell className={`${styles.eventCell} ${isHighlighted ? styles.highlighted : ''}`}>
            <span className={styles.eventDate}>{formatted}</span>
            <span className={styles.eventAuthor}>{author || unknownLabel}</span>
        </TableBodyCell>
    );
};

const ContentActivityTable = ({activity, total, siteKey, language, t}) => {
    const [sortColumn, setSortColumn] = useState('lastModified');
    const [sortDirection, setSortDirection] = useState('desc');
    const [activeFilters, setActiveFilters] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    const rows = useMemo(() => activity || [], [activity]);

    const filteredRows = useMemo(() => {
        if (activeFilters.length === 0) {
            return rows;
        }

        return rows.filter(row => activeFilters.some(filterKey => row[filterKey]));
    }, [rows, activeFilters]);

    const sortedRows = useMemo(
        () => [...filteredRows].sort((left, right) => compareRows(left, right, sortColumn, sortDirection)),
        [filteredRows, sortColumn, sortDirection]
    );

    const pageRows = useMemo(() => {
        const start = (currentPage - 1) * rowsPerPage;

        return sortedRows.slice(start, start + rowsPerPage);
    }, [sortedRows, currentPage, rowsPerPage]);

    const handleSort = columnKey => {
        if (columnKey === sortColumn) {
            setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
        } else {
            setSortColumn(columnKey);
            setSortDirection('desc');
        }

        setCurrentPage(1);
    };

    const handleFilterToggle = filterKey => {
        setActiveFilters(previous => (
            previous.includes(filterKey) ?
                previous.filter(key => key !== filterKey) :
                [...previous, filterKey]
        ));
        setCurrentPage(1);
    };

    const isCapped = total > rows.length;
    const unknownLabel = t('result.activityTable.unknownAuthor');

    return (
        <Collapsible
            isDefaultExpanded={false}
            className={styles.collapsible}
            label={t('result.activityTable.title', {total: total})}
        >
            <div className={styles.body}>
                {isCapped && (
                    <div className={styles.cappedNotice}>
                        {t('result.activityTable.capped', {shown: rows.length, total: total})}
                    </div>
                )}

                <div className={styles.toolbar}>
                    <div className={styles.filters}>
                        {EVENT_FILTERS.map(filter => (
                            <CheckboxItem
                                key={filter.key}
                                id={`content-activity-filter-${filter.key}`}
                                label={t(filter.labelKey)}
                                checked={activeFilters.includes(filter.key)}
                                onChange={() => handleFilterToggle(filter.key)}
                            />
                        ))}
                    </div>
                    <div className={styles.actions}>
                        <Button
                            size="small"
                            variant="outlined"
                            icon={<Download/>}
                            label={t('result.activityTable.exportCsv')}
                            disabled={sortedRows.length === 0}
                            onClick={() => exportActivityToCSV(sortedRows, t)}
                        />
                    </div>
                </div>

                {sortedRows.length === 0 ? (
                    <div className={styles.noResults}>{t('result.activityTable.noActivity')}</div>
                ) : (
                    <>
                        <Table className={styles.table}>
                            <TableHead>
                                <TableRow>
                                    {SORTABLE_COLUMNS.map(column => (
                                        <TableHeadCell
                                            key={column.key}
                                            className={styles.sortableHeader}
                                            onClick={() => handleSort(column.key)}
                                        >
                                            <span className={styles.headerContent}>
                                                {t(column.labelKey)}
                                                {sortColumn === column.key && (
                                                    <span className={styles.sortIcon}>
                                                        {sortDirection === 'asc' ? '▲' : '▼'}
                                                    </span>
                                                )}
                                            </span>
                                        </TableHeadCell>
                                    ))}
                                    <TableHeadCell>{t('result.activityTable.columns.activity')}</TableHeadCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {pageRows.map(row => {
                                    const jContentUrl = buildJContentUrl(row.path, siteKey, language);

                                    return (
                                        <TableRow key={row.path}>
                                            <TableBodyCell className={styles.nameCell}>{row.name || '-'}</TableBodyCell>
                                            <TableBodyCell className={styles.pathCell} title={row.path || undefined}>
                                                {jContentUrl ? (
                                                    <a
                                                        href={jContentUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className={styles.pathLink}
                                                    >
                                                        {row.path}
                                                        <OpenInNew size="small"/>
                                                    </a>
                                                ) : row.path}
                                            </TableBodyCell>
                                            <TableBodyCell className={styles.typeCell}>{row.type || '-'}</TableBodyCell>
                                            {renderEventCell(row.created, row.createdBy, row.isNew, unknownLabel)}
                                            {renderEventCell(row.lastModified, row.lastModifiedBy, row.isModified, unknownLabel)}
                                            {renderEventCell(row.lastPublished, row.lastPublishedBy, row.isPublished, unknownLabel)}
                                            <TableBodyCell>
                                                <span className={styles.tags}>
                                                    {row.isNew && (
                                                        <span className={`${styles.tag} ${styles.tagNew}`}>
                                                            {t('result.activityTable.filters.created')}
                                                        </span>
                                                    )}
                                                    {row.isModified && (
                                                        <span className={`${styles.tag} ${styles.tagModified}`}>
                                                            {t('result.activityTable.filters.modified')}
                                                        </span>
                                                    )}
                                                    {row.isPublished && (
                                                        <span className={`${styles.tag} ${styles.tagPublished}`}>
                                                            {t('result.activityTable.filters.published')}
                                                        </span>
                                                    )}
                                                </span>
                                            </TableBodyCell>
                                        </TableRow>
                                    );
                                })}
                            </TableBody>
                        </Table>
                        <TablePagination
                            currentPage={currentPage}
                            totalNumberOfRows={sortedRows.length}
                            rowsPerPage={rowsPerPage}
                            rowsPerPageOptions={[10, 25, 50, 100]}
                            label={{
                                rowsPerPage: t('result.activityTable.rowsPerPage'),
                                of: t('result.activityTable.of')
                            }}
                            onPageChange={setCurrentPage}
                            onRowsPerPageChange={setRowsPerPage}
                        />
                    </>
                )}
            </div>
        </Collapsible>
    );
};

ContentActivityTable.propTypes = {
    activity: PropTypes.arrayOf(PropTypes.shape({
        name: PropTypes.string,
        path: PropTypes.string,
        type: PropTypes.string,
        created: PropTypes.string,
        createdBy: PropTypes.string,
        lastModified: PropTypes.string,
        lastModifiedBy: PropTypes.string,
        lastPublished: PropTypes.string,
        lastPublishedBy: PropTypes.string,
        isNew: PropTypes.bool,
        isModified: PropTypes.bool,
        isPublished: PropTypes.bool
    })),
    total: PropTypes.number,
    siteKey: PropTypes.string,
    language: PropTypes.string,
    t: PropTypes.func.isRequired
};

export default ContentActivityTable;
