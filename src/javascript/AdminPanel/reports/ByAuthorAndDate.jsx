import React, {useCallback, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {Button, Checkbox, Input, Paper, Typography} from '@jahia/moonstone';
import axios from 'axios';
import {GET_ALL_USERS_QUERY} from '../../graphql/queries';
import globalStyles from '../../styles/global.module.scss';

// Simple Fieldset wrapper component
const Fieldset = ({label, id, children, style}) => (
    <div className={globalStyles.fieldGroup} id={id} style={style}>
        {label && (
            <Typography variant="body" weight="bold" component="label" htmlFor={id} style={{fontSize: '14px', marginBottom: '6px', display: 'block', color: '#12264d'}}>
                {label}
            </Typography>
        )}
        {children}
    </div>
);

Fieldset.propTypes = {
    children: PropTypes.node,
    id: PropTypes.string,
    label: PropTypes.string,
    style: PropTypes.object
};

const ByAuthorAndDate = ({definitions, fields, onFieldChange, onOpenPathPicker, t, baseContentPath}) => {
    const [users, setUsers] = useState([]);
    const [loadingUsers, setLoadingUsers] = useState(false);

    const getDefinition = useCallback(name => (definitions || []).find(field => field.name === name), [definitions]);

    const typeSearchField = getDefinition('typeSearch');
    const typeDateField = getDefinition('typeDateSearch');
    const typeAuthorSearchField = getDefinition('typeAuthorSearch');

    // Fetch users from GraphQL
    useEffect(() => {
        const fetchUsers = async () => {
            setLoadingUsers(true);
            try {
                const response = await axios.post(
                    `${window.contextJsParameters.contextPath}/modules/graphql`,
                    {query: GET_ALL_USERS_QUERY},
                    {
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    }
                );

                if (response.data?.data?.jcr?.nodesByQuery?.nodes) {
                    const rawUsers = response.data.data.jcr.nodesByQuery.nodes;
                    // Transform to expected format: {username, email}
                    const userList = rawUsers.map(user => ({
                        username: user.name,
                        email: user.property?.value || ''
                    }));
                    setUsers(userList);
                }
            } catch (error) {
                console.error('Error fetching users:', error);
            } finally {
                setLoadingUsers(false);
            }
        };

        fetchUsers();
    }, []);

    const renderRadioList = (name, options, groupName) => {
        const optionList = options || [];

        return (
            <div className={globalStyles.radioGroup}>
                {optionList.map(option => (
                    <label key={`${groupName || name}-${option.value}`} className={globalStyles.radioOption}>
                        <input
                            type="radio"
                            name={groupName || name}
                            value={option.value}
                            checked={fields[name] === option.value}
                            onChange={event => onFieldChange(name, event.target.value)}
                        />
                        <span>{t(option.labelKey)}</span>
                    </label>
                ))}
            </div>
        );
    };

    return (
        <Paper hasPadding className={globalStyles.sectionCard}>
            <div style={{paddingBottom: '16px', borderBottom: '1px solid #e8ecf0', marginBottom: '24px'}}>
                <Typography variant="heading" weight="bold" style={{fontSize: '18px', color: '#1a2332', marginBottom: '6px'}}>
                    {t('menu.byAuthorAndDate')}
                </Typography>
                {t('descriptions.byAuthorAndDate') && (
                    <Typography variant="body" style={{color: '#5c6f90', fontSize: '14px'}}>
                        {t('descriptions.byAuthorAndDate')}
                    </Typography>
                )}
            </div>

            {typeSearchField && (
                <Fieldset label={t(typeSearchField.labelKey)} id="report20-type-search" style={{marginBottom: '24px'}}>
                    <div className={globalStyles.fieldGroup}>
                        {renderRadioList('typeSearch', typeSearchField.options, 'report20-typeSearch')}
                    </div>
                </Fieldset>
            )}

            <Fieldset label={t('fields.path')} id="report20-path" style={{marginBottom: '24px'}}>
                <div className={globalStyles.pathRow}>
                    <Input
                        value={fields.pathTxt || baseContentPath || ''}
                        className={globalStyles.growInput}
                        size="big"
                        onChange={event => onFieldChange('pathTxt', event.target.value)}
                    />
                    <Button
                        size="big"
                        variant="outlined"
                        label={t('actions.browse')}
                        onClick={() => onOpenPathPicker('pathTxt', fields.pathTxt || baseContentPath)}
                    />
                </div>
                <Typography variant="caption" className={globalStyles.helperText}>
                    {baseContentPath}
                </Typography>
            </Fieldset>

            <Fieldset label={t('fields.dateRange')} id="report20-date-range" style={{marginBottom: '24px'}}>
                <div className={globalStyles.fieldGroup}>
                    <label className={globalStyles.checkboxRow}>
                        <Checkbox
                            checked={Boolean(fields.searchByDate)}
                            value="searchByDate"
                            onChange={(event, value, checked) => onFieldChange('searchByDate', checked)}
                        />
                        <span>{t('fields.searchByDate')}</span>
                    </label>

                    {Boolean(fields.searchByDate) && (
                        <>
                            {typeDateField && (
                                <div style={{marginTop: '12px'}}>
                                    {renderRadioList('typeDateSearch', typeDateField.options, 'report20-typeDateSearch')}
                                </div>
                            )}
                            <div className={globalStyles.inlineFields} style={{marginTop: '12px'}}>
                                <div className={globalStyles.fieldGroup} style={{flex: 1}}>
                                    <Typography variant="caption" weight="semiBold" style={{fontSize: '13px', marginBottom: '4px', display: 'block'}}>
                                        {t('fields.dateBegin')}
                                    </Typography>
                                    <Input
                                        type="date"
                                        value={fields.dateBegin || ''}
                                        style={{width: '100%'}}
                                        size="big"
                                        onChange={event => onFieldChange('dateBegin', event.target.value)}
                                    />
                                </div>
                                <div className={globalStyles.fieldGroup} style={{flex: 1}}>
                                    <Typography variant="caption" weight="semiBold" style={{fontSize: '13px', marginBottom: '4px', display: 'block'}}>
                                        {t('fields.dateEnd')}
                                    </Typography>
                                    <Input
                                        type="date"
                                        value={fields.dateEnd || ''}
                                        style={{width: '100%'}}
                                        size="big"
                                        onChange={event => onFieldChange('dateEnd', event.target.value)}
                                    />
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </Fieldset>

            <Fieldset label={t('fields.authorFilters')} id="report20-author-filters">
                <div className={globalStyles.fieldGroup}>
                    <label className={globalStyles.checkboxRow}>
                        <Checkbox
                            checked={Boolean(fields.searchAuthor)}
                            value="searchAuthor"
                            onChange={(event, value, checked) => onFieldChange('searchAuthor', checked)}
                        />
                        <span>{t('fields.searchByAuthor')}</span>
                    </label>

                    {Boolean(fields.searchAuthor) && (
                        <>
                            {typeAuthorSearchField && (
                                <div style={{marginTop: '12px'}}>
                                    {renderRadioList('typeAuthorSearch', typeAuthorSearchField.options, 'report20-typeAuthorSearch')}
                                </div>
                            )}
                            <div className={globalStyles.fieldGroup} style={{marginTop: '12px'}}>
                                <Typography variant="caption" weight="semiBold" style={{fontSize: '13px', marginBottom: '4px', display: 'block'}}>
                                    {t('fields.username')}
                                </Typography>
                                <select
                                    value={fields.searchUsername || ''}
                                    disabled={loadingUsers}
                                    style={{
                                        width: '100%',
                                        height: '40px',
                                        padding: '8px 12px',
                                        fontSize: '14px',
                                        border: '1px solid #d0d7e5',
                                        borderRadius: '4px',
                                        backgroundColor: loadingUsers ? '#f5f7fa' : '#ffffff',
                                        cursor: loadingUsers ? 'not-allowed' : 'pointer',
                                        color: '#2c3e5d'
                                    }}
                                    onChange={event => onFieldChange('searchUsername', event.target.value)}
                                >
                                    <option value="">
                                        {loadingUsers ? t('common.loading') || 'Loading...' : t('fields.selectUser') || 'Select a user...'}
                                    </option>
                                    {users.map(user => (
                                        <option key={user.username} value={user.username}>
                                            {user.username} {user.email ? `(${user.email})` : ''}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </>
                    )}
                </div>
            </Fieldset>
        </Paper>
    );
};

ByAuthorAndDate.propTypes = {
    baseContentPath: PropTypes.string,
    definitions: PropTypes.array,
    fields: PropTypes.object.isRequired,
    onFieldChange: PropTypes.func.isRequired,
    onOpenPathPicker: PropTypes.func.isRequired,
    t: PropTypes.func.isRequired
};

export default ByAuthorAndDate;
