# Changelog

All notable changes to this module are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the
module follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

This file starts at the change below. For releases before it, see the
[git tags](https://github.com/Jahia/contentReportReact/tags) and their commit history.

## [Unreleased]

## [2.1.0] - 2026-09-04

### Added

- **Modules deployed on site** (System category). One row per module deployed on the selected
  site: id, name, version, module type, bundle state, how it is deployed — the site's template
  set, a module installed on the site, or a dependency pulled in transitively — and how many
  component types it declares.

- **Component usage** (System category). For every deployed module, each component type it
  declares and how many nodes of exactly that primary type exist under the site. Components
  with zero instances are listed too; that is the point of the report. Counts run on the
  caller's session, so JCR ACLs apply.

### Changed

- Report tables now honour a column's declared `type` when sorting (`number`, `date`,
  `boolean`), and render a numeric `0` as `0` rather than as an empty cell. Columns without a
  declared type keep the previous positional behaviour, so existing reports sort exactly as
  before.
- `.eslintrc.json` is now a root config, so a checkout nested inside another checkout of the
  module (as `maven-release-plugin` does) no longer picks up the parent's ESLint plugins twice.

### Notes

- A *component* is a concrete, non-mixin node type extending `jnt:content`, resolved from the
  node type registry by module id. Templates, mixins and page types are not counted.
- Component usage runs one `rep:count` query per component type. Fast on an indexed
  repository; proportionally more work on a site with many modules.

## [2.0.2] - 2026-09-02

### Added

- **Site overview — detailed content activity log.** The Content Activity block showed
  only counters; it now also lists the content behind them. A collapsible table reports
  every `jmix:editorialContent` node created, modified or published in the last 30 days,
  with its creation, last-modification and last-publication dates and the user
  responsible for each event. Per-row tags mark which of the three events actually fell
  inside the window.

  The table is sortable on any column, filterable per event type, paginated, and each
  path links into jContent. Results are capped at the 500 most recently touched items,
  with the full total reported separately so a truncated list says so.

- **CSV and JSON export** for the activity log, matching the export options the other
  report tables already offer. The JSON payload is projected through an explicit field
  list, so it carries the reported columns only, in a stable order.

- **GraphQL:** `recentActivity` and `recentActivityTotal` on
  `admin.contentReports.overview`. Purely additive — no existing field changed shape or
  nullability.

### Notes

- The activity list is built from two JCR-SQL2 queries merged by node identifier:
  `jmix:editorialContent` filtered on `jcr:created`/`jcr:lastModified`, and
  `jmix:lastPublished` filtered on `j:lastPublished`. The second query is required
  because publishing updates neither `jcr:created` nor `jcr:lastModified`, so a node
  published inside the window can fall outside the first query.
- Queries run on the caller's session (`getCurrentUserSession`), so JCR ACLs filter the
  rows. No system session is used, and the table cannot surface content the current user
  is not allowed to read.
- The existing `publishedContentLast30Days` counter also counts technical nodes such as
  a site's `/users` and `/groups` folders, so it can read higher than the number of rows
  in the table. The counter is unchanged; only the new table excludes them.

## [2.0.1] - 2026-06-15

## [2.0.0] - 2026-04-30

## [1.1.0] - 2026-04-29

[Unreleased]: https://github.com/Jahia/contentReportReact/compare/content-reports-react-2.1.0...HEAD
[2.1.0]: https://github.com/Jahia/contentReportReact/compare/content-reports-react-2.0.2...content-reports-react-2.1.0
[2.0.2]: https://github.com/Jahia/contentReportReact/compare/content-reports-react-2.0.1...content-reports-react-2.0.2
[2.0.1]: https://github.com/Jahia/contentReportReact/compare/content-reports-react-2.0.0...content-reports-react-2.0.1
[2.0.0]: https://github.com/Jahia/contentReportReact/compare/contentReportReact-1.1.0...content-reports-react-2.0.0
[1.1.0]: https://github.com/Jahia/contentReportReact/releases/tag/contentReportReact-1.1.0
