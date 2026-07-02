# Sprint 13D Implementation Notes: Project Explicit Viewer Subject Cleanup

Sprint 13D removes Project read-path overloads that silently treated the authenticated human as the business viewer.

## Scope

- Removed legacy `ProjectService` read overloads that accepted only `Long viewerId`:
  - `listMyProjects(Long viewerId)`
  - `getProjectDetail(Long viewerId, Long projectId)`
  - `shareLink(Long viewerId, Long projectId)`
  - `listSidebarTasks/Payments/Files(Long viewerId, Long projectId)`
- Updated `ProjectController` read endpoints to reject missing or `SYSTEM` viewer subjects with `project.viewer.invalid`.
- Kept `principalHumanId` as the authenticated session owner and liability/authorization anchor.
- Updated `ProjectServiceImpl` to require an explicit authorized viewer subject for project list/detail/sidebar/share read paths.
- Removed principal-human aggregate fallback from project read visibility.
- Added controller and service actor-contract coverage for explicit viewer dispatch and invalid viewer rejection.
- Added a static guard for `Project service legacy human-default viewer overload`.

## Boundary Decision

Project visibility is a business-subject concern. A logged-in human may view as themselves or as an owned/authorized agent, but the service must not infer that choice from the JWT principal.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
