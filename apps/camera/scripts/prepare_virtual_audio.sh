#!/usr/bin/env bash
# File internal version: 0.1.0
set -euo pipefail

if ! command -v pulseaudio >/dev/null 2>&1 || ! command -v pactl >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y pulseaudio pulseaudio-utils
fi

pulseaudio --check >/dev/null 2>&1 || pulseaudio --start --exit-idle-time=-1

for attempt in $(seq 1 20); do
  if pactl info >/dev/null 2>&1; then
    break
  fi
  sleep 0.5
done
pactl info

if ! pactl list short sinks | awk '{print $2}' | grep -qx 'goreecloud_virtual_audio'; then
  pactl load-module module-null-sink \
    sink_name=goreecloud_virtual_audio \
    sink_properties=device.description=GoreeCloudVirtualAudio >/dev/null
fi

pactl set-default-sink goreecloud_virtual_audio
pactl set-default-source goreecloud_virtual_audio.monitor
pactl list short sinks
pactl list short sources

actual_source="$(pactl get-default-source)"
test "$actual_source" = "goreecloud_virtual_audio.monitor"

echo "PULSE_SINK=goreecloud_virtual_audio" >> "$GITHUB_ENV"
echo "PULSE_SOURCE=goreecloud_virtual_audio.monitor" >> "$GITHUB_ENV"
