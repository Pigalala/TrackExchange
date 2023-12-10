# TrackExchange
⚠️ **This plugin is in an experimental state and should be used with caution.**
***

TrackExchange is an addon for [TimingSystem](https://github.com/Makkuusen/TimingSystem) which allows you to copy and paste tracks.

These copied tracks are saved as `.trackexchange` files and can be transferred between servers if needed.

## How to use

### Installing the plugin
Download a release which matches your version of TimingSystem [here](https://github.com/Pigalala/TrackExchange/releases) and place the .jar file in your server plugins folder. This should not fail if TimingSystem is also installed and working properly 🙏
### Using TrackExchange
Currently, there are two commands in Trackexchange:

`/trackexchange copy <track> [saveas]` which copies a track `<track>` and saves it to a file (named `<saveas>.zip`) in the TrackExchange plugin folder. This has the permission node `trackexchange.export`.

_You are not required to use a WorldEdit selection when copying tracks._

`/trackexchange paste <filename> [loadas]` which pastes a track from a file `<filename>` and loads it with the name `[loadas]` if provided, otherwise it takes the name from `<filename>`. This has the permission node `trackexchange.import`.
