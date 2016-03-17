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
This plugin was built with JDK 1.7 and idea 15 version.

How to install it?
-------------
Download this plugin from your IDE (Reviewboard Plugin)

Project Setup
-------------
Required Plugins:
* Git Integration
* Subversion Integration

JDK: 1.7

You'll need to setup the appropriate SDK. IntelliJ SDK and plugin dependencies are required to be setup.

Currently we're developing against version 15

* Go to File -> Project Structure
* Click on SDKs
* Click on plus icon at top of second pane -> IntelliJ IDEA Plugin SDK
* Browse to home of IntelliJ IDEA 15
* It should be named 'IDEA-IU-XXXX'
* Open Libraries and create libraries for git4idea and svn4idea
    * These will be found in \<IDEA dir\>/plugins/(git4idea|svn4idea)/lib/
    * You can add them to the module on creation
* Click ok
