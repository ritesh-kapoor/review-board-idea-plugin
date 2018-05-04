# Reviewboard Plugin for Intellij
- [Download](https://plugins.jetbrains.com/plugin/7872)

Description
-------------
This plugin integrates Reviewboard in intellij for code reviews. 
This plugin tries to ease interaction with ReviewBoard server from the IntelliJ IDE.

Features
-------------
* Do reviews directly from your IDE
* View all, pending or submitted reviews
* Compare (Diff) changes in review locally
* Submit changes to the reviewboard server
* Comment on reviews
* Submit/Discard Reviews

Limitations
-------------
* Viewing multiple reviews is not supported
* Updating diff is not supported

Plugin Compatibility
-------------
This plugin was built with JDK 1.8 and idea 2018.1 version.

How to install it?
-------------
Download this plugin from your IDE (Reviewboard Plugin)

Project Setup
-------------
Required Plugins:
* Git Integration
* Subversion Integration

JDK: 1.8

You'll need to setup the appropriate SDK. IntelliJ SDK and plugin dependencies are required to be setup.

Currently we're developing against version 18

* Go to File -> Project Structure
* Click on SDKs
* Click on plus icon at top of second pane -> IntelliJ IDEA Plugin SDK
* Browse to home of IntelliJ IDEA 18
* It should be named 'IDEA-IU-XXXX'
* Click ok

How To Test
----------
* Install docker
* run "docker-compose -f docker-compose.yml up" to start review board in docker
* Use username: admin and password: admin
* Add any Git/Svn repository
* Use http://localhost:8000 as url