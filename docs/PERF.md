# Performance instrumentation

## Profile pull-to-refresh

`ProfileViewModel.refresh()` emits a single structured log line at the end of every refresh, tagged `AniSyncPerf`. It carries per-phase timings so a slow refresh can be attributed to a specific GraphQL query, the favourites pagination, or the active tab fetch — instead of one opaque total.

### Capture on a connected device

```bash
adb logcat -c
adb logcat -s AniSyncPerf
```

Trigger pull-to-refresh in the app. One line per refresh is printed, for example:

```
I AniSyncPerf: profile.refresh own=true tab=OVERVIEW total=2143ms refreshJob=1980ms profileQuery=412ms activities=380ms favoritesTotal=1180ms favoritesFirstPage=310ms favoritesRest=870ms favoritesPages=5 activeTab=120ms followState=skipped forceNetwork=true
```

Field reference:

| Field | Meaning |
|-------|---------|
| `own` | `true` if viewing own profile, `false` for another user |
| `tab` | Active `ProfileTab` at refresh start |
| `total` | Wall-clock from `isRefreshing=true` to `isRefreshing=false` |
| `refreshJob` | Time inside the profile refresh job (`profileQuery` + `activities` + `favoritesTotal` overlap-aware) |
| `profileQuery` | `GetUserProfileQuery` round-trip |
| `activities` | `GetUserActivitiesQuery` round-trip (runs parallel with favourites) |
| `favoritesTotal` | `favoritesFirstPage` + `favoritesRest` |
| `favoritesFirstPage` | Sequential page-1 fetch (used to learn `lastPage`) |
| `favoritesRest` | Pages 2..N (parallel, `Semaphore(4)`) |
| `favoritesPages` | Total page count fetched |
| `activeTab` | Tab-specific fetch job (`GetUserLibraryQuery`, stats, reviews, social) |
| `followState` | `getFollowState` round-trip; `skipped` on own profile |
| `forceNetwork` | `true` for pull-to-refresh, `false` for cold-open cache-first |

Other-user profile path runs the actual fetch in a separate flow (`targetRefreshSignal` → `profileState`), so `profileQuery`/`activities`/`favoritesTotal` will show `-1`. The `total` and `activeTab` fields are still meaningful there.

### Perfetto trace (deeper inspection)

Each phase is also wrapped in an `android.os.Trace` section. Slice names:

- `AniSync.Profile.Refresh.Total`
- `AniSync.Profile.Refresh.RefreshJob`
- `AniSync.Profile.Refresh.ActiveTabJob.<TabName>`
- `AniSync.Profile.Refresh.FollowStateJob`
- `AniSync.Profile.Query.UserProfile`
- `AniSync.Profile.Query.UserActivities`
- `AniSync.Profile.Query.Favorites.Page<N>`

Capture via `adb shell perfetto -o /data/misc/perfetto-traces/trace -t 15s sched,freq,gfx,view,wm,am,sm,res,binder_driver,hal,dalvik,camera,input` (or any standard Android system trace config that includes `sched`), pull the file, and open it at https://ui.perfetto.dev. Slices appear under the AniSync process tree.

### What to attach to an issue

Run `adb logcat -s AniSyncPerf`, reproduce the slow refresh 3–5 times, paste the resulting lines into the issue. Include device model, Android version, and network type (Wi-Fi / mobile).
