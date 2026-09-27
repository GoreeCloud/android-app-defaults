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

for receiver in application.findall("receiver"):
    name = receiver.attrib.get(android + "name")
    exported = receiver.attrib.get(android + "exported")
    if name == ".system.RescheduleReceiver":
        if exported != "true":
            raise SystemExit("RescheduleReceiver must receive protected system lifecycle broadcasts.")
    elif exported != "false":
        raise SystemExit(f"Internal receiver must not be exported: {name}")

print("Clock manifest privacy and component-export boundary verified.")
