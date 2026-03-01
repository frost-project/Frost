# Frost

Frost is a [Hyphanet](https://www.hyphanet.org/) client that provides newsgroup-like messaging, private encrypted messages, file upload and download functionality and a file sharing system.

## Requirements

- [OpenJDK](https://openjdk.org/) 17 or newer
- [Hyphanet](https://www.hyphanet.org/)

## Build

1. Download the source code and extract it.
2. Open a command prompt in the root-directory of the extracted source code.
3. Run the following command: `gradlew distZip`. This will create the zip-archive `build/distributions/Frost.zip`.
4. Extract the generated zip-archive.

## Update

Remove the folder `lib` and the files `Frost` and `Frost.bat` (if exists). Extract the content of the archive to your Frost folder and overwrite existing files.

## Run

Run Frost with `Frost` (Linux, macOS) or `Frost.bat` (Windows).

To create a shortcut that hides the console, use:

- `create-shortcut.sh` (Linux) → creates `Frost.desktop`
- `create-shortcut.bat` (Windows) → creates `Frost.lnk`

## Contact

Project leader: Spider-Admin

Authors: Frost development team (see `Help -> About -> More` for details).

### Spider-Admin

Freemail: spider-admin@tlc66lu4eyhku24wwym6lfczzixnkuofsd4wrlgopp6smrbojf3a.freemail [^2]

Frost: Spider-Admin@Z+d9Knmjd3hQeeZU6BOWPpAAxxs

FMS: Spider-Admin

Sone: [Spider-Admin](http://localhost:8888/Sone/viewSone.html?sone=msXvLpwmDqprlrYZ5ZRZyi7VUcWQ~Wisznv9JkQuSXY) [^1]

I do not regularly read the email associated with GitHub.

## License

Frost by Frost Project is licensed under the [GNU General Public License, Version 3.0](https://www.gnu.org/licenses/gpl-3.0.html.en).

[^1]: Link requires a running Hyphanet node at http://localhost:8888/
[^2]: Freemail requires a running Hyphanet node
