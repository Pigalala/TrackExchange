# TrackExchange

TrackExchange is an addon for [TimingSystem](https://github.com/Makkuusen/TimingSystem) which allows you to copy and paste tracks.

These copied tracks are saved as `.trackexchange` files and can be transferred between servers if needed.

# How to use

## Installing the plugin
Download a release which matches your version of TimingSystem [here](https://github.com/Pigalala/TrackExchange/releases) and place the .jar file in your server plugins folder.

Ensure that FastAsyncWorldEdit is also running on your server.
## Using TrackExchange
Currently, there are two main commands in Trackexchange:
### Exporting
TrackExchange allows you to copy a track with or without a WorldEdit schematic. A schematic saves if you have a WorldEdit selection whilst running the command:

`/trackexchange copy <track> [saveas]`: copies a track named `<track>` and saves it to a file (named `<saveas>.trackexchange`) in the TrackExchange plugin folder.

This has the permission node `trackexchange.export`.
### Importing
`/trackexchange paste <filename> [loadas]` which pastes a track from a file named `<filename>` and loads it with the name `[loadas]` if provided, otherwise it takes the name from `<filename>`. 

This has the permission node `trackexchange.import`.

# Building
*JDK 21 is required*
1. Clone the repository.
2. Run `./gradlew build`
3. The artifact can be found in `./build/libs/`
