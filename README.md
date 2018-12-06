# cc
Calendar software designed with the cycling community in mind:
* Display cycling events
* Create cycling events
* Display training events
* Create training events
* Option to repeat regular training events every week ex. repeat automatically the monday event for next monday.
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
## Instructions

1. clone the repository
2. In the cc/src/cc/models/cdb.clj file, you will find a way of creating all database tables needed with sample data.
3. Create the following folder on: cc/resources/private
4. Create a configuration file on: cc/resources/private/config.clj

Example configuration(cc/resources/private/config.clj):

`
{:db-protocol 		"mysql"
 :db-name 		"//localhost:3306/cc?characterEncoding=UTF-8"
 :db-user		"your-user-here"
 :db-pwd		"your-password-here"
 :db-class		"com.mysql.cj.jdbc.Driver"
 :email-host		"your-email-smtp-server"
 :email-user		"your-email-user"
 :email-password 	"your-email-password"
 :port			3000
 :tz			"US/Pacific"
 :site-name		"Site Name"
 :base-url		"http://0.0.0.0:3000/"
 :uploads		"./uploads"
 :path			"/uploads/"}`

## Prerequisites

You will need:
* [Leiningen][] 2.0.0 or above installed
* jdk8 or above
* MySQL

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run

## Demo
http://lucero-systems.cf

## License

Copyright © 2019 LS
# Cuadrantes
