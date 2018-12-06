# cc
Calendar software designed with the cycling community in mind:
* Display cycling events
* Create cycling events
* Display training events
* Create training events
* Allow any user to confirm asistance to any training event
	* When confirming assistance on a training event the creator of the event will receive an email with details entered by the confirming cyclist
	* When the creator of the event deletes the event, all users who confirmed attendance will receive an email that the training event has been cancelled.
* Anonymous users
	* Can see all events and training events on the Event and Training Calendars
	* Can create only anonymous training events
	* Those events can be seen by everyone
	* All anonymous users can create/delete/modify anonymous training events
	* Can only view cycling events, but not modify them.
* Regular users
	* Can see all events and training events on the Event and Training Calendars
	* can create training events stamped with their email
	* can only delete the training events created by them.
	* Can create cycling events stamped with their email
	* Can only delete the cycling events created by them.
* Administrator users
	* Can see all events and training events on the Event and Training Calendars
	* can create training events stamped with their email
	* can only delete the training events created by them.
	* Can create cycling events stamped with their email
	* Can only delete the cycling events created by them.
	* Can create cycling club information
	* Can modify/delete any cycling club information
* System users
	* God mode - can do everything that is possible in the software...
## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed and java jdk8 or above.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run

## License

Copyright © 2019 LS
# Cuadrantes

## Demo
http://lucero-systems.cf
