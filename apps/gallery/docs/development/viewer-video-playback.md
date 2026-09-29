# Native Viewer Video Playback — Development Boundary

## Status

Development integration candidate. This document does not establish Release Candidate or Stable acceptance.

## Implemented boundary

The normal Gallery viewer may present an already-authorized local `MediaItem` as either a static image or native Android video playback according to `GalleryViewerPlaybackPolicy`.

Video playback:

- accepts only the shared canonical Android MediaStore image/video item URI boundary;
- uses the existing local `playVideosAutomatically` and `loopVideos` preferences;
- pauses playback when the Activity loses the foreground and resumes only when that host pause actually interrupted active playback;
- stops the active player when navigating to another media item, closing the viewer, destroying the Activity, or completing a confirmed destructive media mutation;
- retains the orientation-aware static viewer image path as the poster/fallback presentation if video loading or playback fails;
- does not add filesystem paths, arbitrary ContentProviders, HTTP/network playback, background playback, casting, DRM, Picture-in-Picture, subtitle, or new permission authority.

## Remaining acceptance

This integration still requires representative physical-device and OEM codec/lifecycle testing, media-revocation and provider-failure drills, rendered accessibility/form-factor acceptance, current Stable Glaze UI consumer acceptance, protected signing/provenance, Release Candidate approval, and governed Stable promotion.
