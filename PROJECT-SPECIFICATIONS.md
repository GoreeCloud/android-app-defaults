# Android App Defaults — Project Specifications

**Repository:** `GoreeCloud/android-app-defaults`  
**Project type:** Android application monorepo  
**Lifecycle:** Development  
**Repository visibility:** Public  
**Default branch:** `main`  
**Current verified main snapshot:** `60031a0632ae169c22c8a2a52eed6b5678646f0d`  
**Primary current application:** GoreeCloud Since (`:apps:since`)  
**Deployment model:** Independently installable Android applications with shared repository infrastructure  
**Licensing:** No repository-local license file was present in the verified root snapshot; applicable GoreeCloud licensing governance still applies.  
**Canonical authority:** This file governs project requirements once accepted on the default branch. `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` provide implementation-state and change-history evidence and must remain consistent with this specification.

## Migration provenance and interpretation

This specification reconciles the former Google Drive **Project Specification — Android App Defaults** (version 1.1.0, last updated September 23, 2026) with verified repository state.

The migrated Drive document was primarily planning-oriented. Planned requirements remain requirements only where they still belong to this repository. Current implementation claims are controlled by verified repository evidence, not by the former Drive document.

At the migration snapshot:
- only `:apps:since` is included by `settings.gradle.kts`;
- `apps/since/` is the only application directory under `apps/`;
- the repository is Development, not Release Candidate, production, or Stable;
- broader Android App Defaults applications remain planned unless current repository evidence states otherwise.

The former Drive specification also contained detailed scope for Calendar, Contacts, Notes, Gallery, and File Manager. Those projects now have independent repositories and are no longer treated as normative monorepo ownership here:
- Calendar → `GoreeCloud/goreecloud-calendar`
- Contacts → `GoreeCloud/contacts`
- Notes → `GoreeCloud/goreecloud-notes`
- Gallery → `GoreeCloud/gallery`
- File Manager → `GoreeCloud/file-manager`

Their Drive-era requirements are migration inputs for those repositories and must not be silently discarded. The source Drive document therefore remains protected from deletion until those split obligations are reconciled at their authoritative destinations.

## Purpose

The purpose of android-app-defaults is to provide a unified home for small Android applications that replace common built-in utilities.
The suite should prioritize:
- Native Android behavior
- Fast startup
- Low memory usage
- Low storage usage
- Offline-first operation
- Minimal permissions
- No advertising
- No behavioral tracking
- No unnecessary accounts
- No unnecessary network connectivity
- Consistent interface design
- Strong accessibility
- Shared design language
- Independent application installation
- Independent application updates
- Long-term maintainability
The applications should feel like parts of one cohesive system rather than unrelated standalone utilities.

## Current verified implementation boundary

Current `main` implements the Android monorepo foundation and GoreeCloud Since Development source. Verified implementation details, exact PR/commit evidence, Room schema state, UI/accessibility evidence, and material limitations are maintained in `IMPLEMENTED-FEATURES.md` and `CHANGELOGS.md`.

The repository must not represent any other Android App Defaults application as implemented until corresponding source and verification evidence exists.

## Required application model

Applications retained in this monorepo must:
- remain independently installable and independently updateable;
- use distinct application identity, versioning, permissions, storage, settings, tests, release artifacts, release notes, and update lifecycle;
- share common foundations only where doing so improves consistency without creating unnecessary runtime coupling;
- remain usable without installation of unrelated GoreeCloud applications;
- remain local-first, offline-capable, privacy-preserving, and permission-minimal wherever technically possible.

## Planned retained application scope

### Clock

A complete clock and time-management application.
Planned capabilities
- Current local time
- Multiple clocks
- World clocks
- Alarm management
- Multiple alarms
- Repeating alarms
- One-time alarms
- Alarm labels
- Alarm sounds
- Vibration controls
- Gradual alarm volume
- Snooze configuration
- Dismiss controls
- Upcoming alarm display
- Timer
- Multiple simultaneous timers
- Timer labels
- Stopwatch
- Lap tracking
- Previous stopwatch results
- Bedside clock mode
- Full-screen clock
- Analog clock option
- Digital clock option
- 12-hour and 24-hour formats
- Time-zone management
- Home-screen widgets
- Lock-screen integration where supported
- Offline operation

### Calculator

A fast general-purpose calculator designed for both simple and advanced calculations.
Planned capabilities
- Basic arithmetic
- Addition
- Subtraction
- Multiplication
- Division
- Percentages
- Parentheses
- Decimal calculations
- Negative numbers
- Calculation history
- Copy results
- Paste values
- Scientific calculator mode
- Trigonometric functions
- Logarithmic functions
- Exponents
- Roots
- Constants
- Degree and radian modes
- Memory functions
- Large-number support
- Expression editing
- Error explanations
- Offline operation
Optional future capabilities may include:
- Unit conversion
- Currency calculation using manually supplied or optionally retrieved rates
- Date calculations
- Programmer calculator
- Binary calculations
- Octal calculations
- Decimal calculations
- Hexadecimal calculations

### Voice Recorder

A local audio recording application.
Planned capabilities
- Audio recording
- Pause and resume
- Recording timer
- Recording names
- Recording folders
- Playback controls
- Variable playback speed
- Trim recordings
- Rename recordings
- Delete recordings
- Share recordings
- Recording quality settings
- Audio format selection
- Microphone selection where supported
- Background recording
- Storage location controls
- Recording metadata
- Waveform visualization
- Local-only operation
Audio should never leave the device unless the user explicitly exports or shares it.

### Compass

A lightweight compass and orientation utility.
Planned capabilities
- Compass heading
- Cardinal directions
- Degree display
- Sensor accuracy indicator
- Calibration guidance
- Magnetic heading
- True heading where available
- Orientation information
- Minimal full-screen mode
- Dark mode
- Optional haptic feedback
The application should function without network connectivity.

### Flashlight

A minimal flashlight controller.
Planned capabilities
- Flashlight on/off
- Brightness control where supported
- Quick toggle
- Home-screen shortcut
- Configurable startup behavior
- Screen-light mode
- Emergency flashing mode
- Optional timer
- Minimal interface
The application should request no permissions beyond those strictly necessary for flashlight operation.

### Since

A local-first elapsed-time, streak, goal, and milestone tracker for Android. I plan to develop it as an original GoreeCloud-owned application at apps/since/ with core operation that does not require network access.
Planned capabilities
- Track elapsed time since permanent or historical events.
- Track resettable streaks while preserving prior periods in history.
- Attach optional duration goals and show progress and estimated completion.
- Display elapsed time in days, weeks, months, or years using calendar-aware time semantics.
- Support search, sorting, archive, restore, delete, and local history.
- Export and import versioned local data through user-selected documents.
- Remain useful after process death and reboot without a continuously running timer service.
- Add home-screen widgets and milestone notifications after the MVP.
- Keep INTERNET permission absent from the MVP.
The detailed requirements, architecture, persistence model, testing plan, and acceptance criteria are governed by Project Specification — Since.

### Possible additional applications

The repository may later include additional small utilities such as:
- Unit Converter
- Sound Recorder
- Ruler
- Level
- Magnifier
- QR scanner
- Document scanner
- Timer utility
- Stopwatch utility
- Simple drawing utility
- Text editor
- Weather display client
- Local task list
- Reminder manager
- Local password generator
- Device information utility
- Storage analyzer
- Battery information utility
Applications should be added only when they fit the repository's purpose as small default-app or utility replacements.

## Monorepo Architecture
The repository should separate individual applications from shared components.
android-app-defaults/
│
├── apps/
│   ├── clock/
│   ├── calculator/
│   ├── calendar/
│   ├── contacts/
│   ├── notes/
│   ├── recorder/
│   ├── compass/
│   ├── flashlight/
│   ├── gallery/
│   ├── file-manager/
│   └── since/
│
├── shared/
│   ├── ui/
│   ├── design/
│   ├── themes/
│   ├── icons/
│   ├── accessibility/
│   ├── preferences/
│   ├── permissions/
│   ├── storage/
│   ├── privacy/
│   ├── localization/
│   └── utilities/
│
├── docs/
│   ├── architecture/
│   ├── design/
│   ├── privacy/
│   ├── standards/
│   └── app-specs/
│
├── tests/
│
├── scripts/
│
└── README.md
	

## Independent Applications
Although the applications share one repository, each application should remain an independent Android application.
Each application should have its own:
- Application package
- Application icon
- Application name
- Version
- Build target
- Release artifact
- Permissions
- Tests
- Storage
- Settings
- Release notes
- Update lifecycle
Installing one application must not require installing the others.
For example, a user should be able to install only:
GoreeCloud Clock
	without also installing:
GoreeCloud Calculator
GoreeCloud Notes
GoreeCloud Gallery
	The monorepo is a development and maintenance structure, not an application bundle requirement.


## Shared Application Foundation
Applications should share a common internal foundation wherever practical.
## Shared UI
The shared UI layer should provide reusable components for:
- Navigation
- Toolbars
- Dialogs
- Sheets
- Menus
- Lists
- Cards
- Buttons
- Toggles
- Selection controls
- Search
- Empty states
- Error states
- Loading states
- Permission requests


## Shared Design System
All applications should follow the same GoreeCloud visual language.
The shared design system should define:
- Typography
- Spacing
- Corner radii
- Elevation
- Motion
- Icons
- Layout behavior
- Light theme
- Dark theme
- High-contrast modes
- Dynamic system color integration where appropriate
Applications should feel related without becoming visually identical when their use cases require different interfaces.


## Shared Settings Framework
Common settings should behave consistently across applications.
Potential shared settings include:
- Theme
- System theme following
- Language
- Haptic feedback
- Animation preferences
- Accessibility options
- Privacy controls
- Backup preferences
- Data import
- Data export
- Reset options
- About information
Application-specific settings should remain within their respective application.


## Privacy Model
The default position of the application suite should be:
Local first, offline capable, and permission minimal.
Applications should not require an account unless a future feature inherently requires one.
Applications should not require internet connectivity when their primary function can operate locally.
**Examples:**
- Calculator calculations remain local.
- Clock alarms remain local.
- Notes remain local unless synchronization is explicitly enabled.
- Recordings remain local unless explicitly shared.
- Gallery media remains local unless the user explicitly connects another storage source.
- Contacts can remain local.
- Calendar events can remain local.


## Permission Model
Every application should follow a least-permission architecture.
A permission should only be requested when:
1. A feature actively requires it.
2. The user attempts to use that feature.
3. The reason for requesting it can be clearly explained.
Applications should avoid requesting broad permissions during first launch when those permissions are not immediately necessary.


## Network Access
Applications that do not require networking should not receive networking capability merely for convenience.
For example:
Calculator → No network required
Clock → No network required
Compass → No network required
Flashlight → No network required
Recorder → No network required
Notes → No network required by default
Gallery → No network required by default
Since → No network required
	Network access should be introduced only for clearly defined optional features.


## Offline-First Design
Core application functionality must remain available without an internet connection whenever technically possible.
An unavailable network connection should not prevent users from:
- Setting alarms
- Performing calculations
- Reading notes
- Recording audio
- Browsing local files
- Viewing local media
- Managing local contacts
- Viewing locally stored calendar events


## Data Ownership
Users should maintain control over data generated by these applications.
Where applicable, applications should support:
- Export
- Import
- Backup
- Restore
- Human-readable formats
- User-selected storage destinations
Data should not intentionally be trapped inside an application when a reasonable portable representation exists.


## Accessibility
Accessibility should be implemented as a shared requirement rather than added individually after applications are completed.
The suite should support:
- Screen readers
- Large text
- Display scaling
- High contrast
- Clear focus indicators
- Keyboard navigation where applicable
- Switch-access compatibility
- Reduced-motion preferences
- Descriptive interface labels
- Adequate touch-target sizes
- Logical navigation order


## Application Integration
Applications may integrate with each other when useful, but they should not depend on each other unnecessarily.
Examples of appropriate integration include:
- Contacts opening the Dialer
- Gallery opening a file through the File Manager
- Recorder saving audio into a user-accessible media directory
- Calendar creating reminders
- Notes attaching locally stored files
Each application should continue functioning independently when another GoreeCloud application is not installed.


## Default-App Integration
Applications capable of functioning as system defaults should support the appropriate operating-system mechanisms.
This may eventually apply to areas such as:
- Phone handling
- Contacts
- Calendar
- Gallery
- File handling
Applications should integrate using documented operating-system mechanisms rather than relying on fragile workarounds.


## Repository Boundary
The repository is intended for small and medium-sized foundational applications.
A project should generally remain inside android-app-defaults when it:
- Is primarily a local utility
- Has relatively limited architecture
- Benefits substantially from shared UI and infrastructure
- Does not require a major backend
- Does not operate as its own platform
- Does not have an unusually complex release lifecycle
A project should receive a separate repository when it grows into a substantially independent system.
Examples of applications maintained separately include:
GoreeCloud/music
GoreeCloud/youtube-player
GoreeCloud/dialer
GoreeCloud/swarm
	These systems may have more substantial playback engines, communication stacks, networking layers, service integrations, or application-specific architecture.


## Graduation Rule
An application can begin inside android-app-defaults and later move into its own repository.
A separate repository should be considered when the application develops:
- A large independent architecture
- A substantial networking stack
- Its own server components
- A major synchronization system
- Independent libraries
- A large development team
- A substantially different release schedule
- A dedicated ecosystem of supporting components
Moving an application out of the monorepo should not require redesigning the application from scratch.
Shared components should therefore maintain clean internal boundaries.


## Repository Naming Standard
Repository names within the GoreeCloud organization must describe the repository itself without repeating the organization name.
### Required pattern
GoreeCloud/<project>
	Examples:
GoreeCloud/android-app-defaults
GoreeCloud/music
GoreeCloud/youtube-player
GoreeCloud/dialer
GoreeCloud/swarm
	Prohibited pattern
GoreeCloud/goreecloud-<project>
	The goreecloud- repository prefix is prohibited because the organization namespace already provides that information.
Repository names should therefore be:
- Short
- Descriptive
- Lowercase
- Hyphenated when multiple words are necessary
- Free of redundant organization prefixes


## Product Naming
Repository naming and application naming are separate concerns.
A repository may use:
android-app-defaults
	while applications presented to users may use names such as:
GoreeCloud Clock
GoreeCloud Calculator
GoreeCloud Calendar
GoreeCloud Contacts
GoreeCloud Notes
GoreeCloud Recorder
GoreeCloud Gallery
GoreeCloud Files
GoreeCloud Since
	The repository name identifies source organization.
The product name identifies the application presented to users.


## Long-Term Direction
android-app-defaults can become the foundation of a cohesive GoreeCloud Android application suite.
Rather than independently recreating basic utilities with different architectures, the repository provides one common foundation for:
- Interface design
- Privacy
- Accessibility
- Local storage
- Permissions
- Preferences
- Application architecture
- Testing
- Release standards
The result should be a collection of small, dependable applications that work independently while feeling like components of the same environment.
The defining principle is:
Small default and utility applications share one foundation; applications that become substantial platforms receive their own repositories.

## Architecture and authoritative data boundaries

The monorepo may share UI, design, themes, icons, accessibility foundations, preferences, permission handling, storage helpers, privacy controls, localization, and utilities. Shared layers must not manufacture provider-owned authorization, security, identity, network, privacy, or synchronization truth.

Application-local data is authoritative for local-only functionality unless an approved integration explicitly delegates authority elsewhere. Networked or synchronized functionality must identify the owning service, permissions, failure behavior, and data exchanged before implementation.

## Security requirements

- Apply least privilege to Android permissions, storage, IPC, background work, and network access.
- Request sensitive permissions only when the user invokes a feature that requires them and explain the purpose.
- Do not add Internet access to applications whose core function does not require networking merely for convenience.
- Do not add advertising, behavioral tracking, or unnecessary telemetry.
- Keep cleartext traffic disabled unless a separately governed requirement explicitly justifies an exception.
- Treat external libraries, build plugins, and supply-chain dependencies as security-sensitive dependencies subject to GoreeCloud governance.
- Keep application signing, release provenance, dependency review, and vulnerability handling as release gates rather than inferred properties of successful source builds.

## Privacy requirements

- Local-first and offline-capable behavior is the default.
- Accounts and remote services are optional unless a feature inherently requires them.
- User-generated data must not leave the device without an explicit user action or an enabled, documented integration.
- Data collection must be minimized.
- Export, import, backup, restore, deletion, and retention behavior must be explicit for each application where applicable.
- Privacy-sensitive features must fail closed when required authority or configuration is unavailable.

## User interface and accessibility requirements

Applicable user-facing applications must use approved GoreeCloud branding and the applicable Glaze UI requirements while preserving application-appropriate identity. They must support accessible navigation, screen readers, scalable text, high contrast, reduced motion, logical focus order, adequate touch targets, clear empty/error/loading states, and responsive behavior.

Repository-local UI evidence is necessary for application acceptance. A shared design-system version or successful build does not by itself establish downstream visual or accessibility conformance.

## Deployment and operations

Each application is built and released independently. The monorepo is a development/maintenance structure, not a bundled-install requirement.

Where CI exists, it must validate the exact candidate source. Source-level and emulator evidence must not be represented as representative-device, production, release, or Stable acceptance unless the applicable acceptance requirements have separately passed.

## Testing and acceptance

The repository must distinguish:
- implemented source;
- automated source/build verification;
- emulator/runtime verification;
- rendered UI/accessibility evidence;
- representative physical-device evidence;
- Release Candidate acceptance;
- production readiness;
- Stable qualification.

Passing CI is necessary evidence where configured but is not sufficient by itself for production or Stable status.

## Maintenance, graduation, and retirement

Applications may graduate to their own repository when they develop substantial independent architecture, networking, servers, synchronization, libraries, release schedules, or ecosystem responsibilities. When graduation occurs:
- move the owning requirements with the application;
- record the transition in `PROJECT-RECORD.md`;
- retain historical Git references where practical;
- update repository documentation and cross-references;
- do not use Google Drive as a fallback project authority.

Retired or superseded applications must retain enough Git history and project record context to explain what existed, why it changed, and where responsibilities or data moved.

## Related repository documentation

- [README.md](README.md)
- [IMPLEMENTED-FEATURES.md](IMPLEMENTED-FEATURES.md)
- [PLANNED-FEATURES.md](PLANNED-FEATURES.md)
- [CHANGELOGS.md](CHANGELOGS.md)
- [PROJECT-RECORD.md](PROJECT-RECORD.md)
