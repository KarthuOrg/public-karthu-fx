# Oven Timer

Counts down the baking time, then rings once and lets the oven light blink.

## Tray in

- **tray** (optional) — the tray to watch.

## Tray out

- **done** — the same tray, as it went in. Without a tray, the timer rings all the same.

## Settings

- **Time** — how long to bake, 0 or more; 25 by default.
- **Unit** — `seconds`, `minutes` (the default) or `hours`.

## While it counts

The ring around the dial fills as the time passes and the label shows the time left, as `HH:MM:SS left`. A stopped
timer ends at once. A paused timer holds, at most a quarter of a second after the pause, and the paused time does not
count: once resumed, the timer counts down the rest of its time.
