Photo App
---
A *really simple* app for reviewing photo fullscreen and grouping them (e.g. to quickly find the best!). When you put a photo into a group, a sub directory is created for the group and a sum-link to your chosen photo is created.

To build:

	mvn install
	
To use:

	java -jar target/photo-app-1.0.0-SNAPSHOT.jar ~/Pictures
	
When done:

	ls -l target/1

Controls:

* `Escape` - Quit
* `1`, `2`, `3` and `4` - Put in a group
* `r` - Rotate 90 degrees