#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

manifest = Path("apps/clock/src/main/AndroidManifest.xml")
root = ET.parse(manifest).getroot()
android = "{http://schemas.android.com/apk/res/android}"

permissions = {
    node.attrib.get(android + "name")
    for node in root.findall("uses-permission")
}
allowed = {
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.VIBRATE",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.SCHEDULE_EXACT_ALARM",
    "android.permission.USE_EXACT_ALARM",
    "android.permission.USE_FULL_SCREEN_INTENT",
}
unexpected = sorted(permission for permission in permissions if permission not in allowed)
if unexpected:
    raise SystemExit(f"Unexpected Clock permissions: {unexpected}")

for forbidden in ("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"):
    if forbidden in permissions:
        raise SystemExit(f"Network permission is forbidden for Clock core: {forbidden}")

application = root.find("application")
if application is None:
    raise SystemExit("Clock manifest is missing <application>.")
if application.attrib.get(android + "allowBackup") != "false":
    raise SystemExit("Clock must keep platform backup disabled until recovery semantics are validated.")
if application.attrib.get(android + "usesCleartextTraffic") != "false":
    raise SystemExit("Clock must explicitly disable cleartext traffic.")

main = None
for activity in application.findall("activity"):
    if activity.attrib.get(android + "name") == ".MainActivity":
        main = activity
        break
if main is None or main.attrib.get(android + "exported") != "true":
    raise SystemExit("Clock MainActivity must be explicitly exported for launcher use.")

public_receivers = {
    ".system.RescheduleReceiver",
    ".widget.ClockWidgetProvider",
    ".widget.AlarmWidgetProvider",
    ".widget.TimerWidgetProvider",
}

for receiver in application.findall("receiver"):
    name = receiver.attrib.get(android + "name")
    exported = receiver.attrib.get(android + "exported")
    if name in public_receivers:
        if exported != "true":
            raise SystemExit(f"System-facing receiver must be exported: {name}")
    elif exported != "false":
        raise SystemExit(f"Internal receiver must not be exported: {name}")

widget_receivers = {
    ".widget.ClockWidgetProvider",
    ".widget.AlarmWidgetProvider",
    ".widget.TimerWidgetProvider",
}
for receiver in application.findall("receiver"):
    name = receiver.attrib.get(android + "name")
    if name not in widget_receivers:
        continue
    actions = {
        action.attrib.get(android + "name")
        for intent_filter in receiver.findall("intent-filter")
        for action in intent_filter.findall("action")
    }
    if actions != {"android.appwidget.action.APPWIDGET_UPDATE"}:
        raise SystemExit(f"Unexpected widget receiver actions for {name}: {sorted(actions)}")
    metadata = receiver.findall("meta-data")
    if not any(
        node.attrib.get(android + "name") == "android.appwidget.provider"
        and node.attrib.get(android + "resource")
        for node in metadata
    ):
        raise SystemExit(f"Widget receiver is missing provider metadata: {name}")

print("Clock manifest privacy and component-export boundary verified.")
