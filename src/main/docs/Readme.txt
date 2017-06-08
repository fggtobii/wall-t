

==== What is it ? ====

This application is a buil radiator to monitor build status over a TeamCity continous integration server.


==== Quick Start ====

    1. Unzip the archive
    2. Define JAVA8_HOME environement variable or edit start script to link to a JRE 8 (or higher)
    3. Launch the application by script start.sh on Linux or start.bat on Windows
    4. Enjoy !


=== More information ? ===

See the project page on internet at: https://code.google.com/p/wall-t/


==== Credits ====

Developed by Cedric Longo. https://plus.google.com/+CédricLongo
Released under the terms of the GNU GENERAL PUBLIC LICENSE V3.

==== Modifications ====

====

Modified by Fredrik Grönberg (2017).
Modification is available here: https://github.com/fggtobii/wall-t.git

launch application with "java -jar bin/Wall-T.jar [Options]"

WallApplication Command Line Options
--help : print help
--config <custom_config_file>.json : runs the application with the custom_config_file.json. Default configuration is config.json
--auto : runs the application with the config.json and connects to the server and switches to wall view automatically
--maximized : starts the application with a maximized application window
--screen : Choose screen index (0 is primary and counting up)

branches can now be specified in the config file and edited in the GUI

====