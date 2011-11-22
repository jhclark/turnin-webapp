Turnin Webapp
=============

Written By Jonathan Clark 2011
For 11-711 at Carnegie Mellon University

Overview
========

A tool for instructors that allows students to submit .tgz files using a web interface.

Features:
* Dead simple web-interface
* Minimal configuration
* Built-in cryptographic receipt system to verify student turn-in claims (in case there is a discrepancy between the file on-disk and a file the student claims to have submitted)
* Tracks multiple submissions

The most recent version of files submitted are kept in data/uploads. The entire history of files submitted by students is kept in data/uploads/all.

Building and Running the WebApp
===============================

This app is built ontop of Scalatra https://github.com/scalatra/scalatra

You'll need Scala 2.9.1+ installed. You probably want to run this as a special user with very few permissions.

```
./build.sh
./run.sh
```

Configuration
=============

TODO

Working with Receipts
=====================

Generating an Encryption Key for Turnin Receipts
------------------------------------------------

```
./keygen.sh key.bin
```

Verifying the Time and MD5 Checksum of a Submission
---------------------------------------------------

Given the receipt (e.g. a362b2bf7cd6dc646e7fabc478208752), you can verify the time of submission.
If the student also provides the file they claim to have submitted, you can also verify the file's contents.

```
./view.sh key.bin a362b2bf7cd6dc646e7fabc478208752
md5sum file_student_claims_submitted.tar.gz
```

Checking for Stolen Receipts
----------------------------

If a student claims the receipt of another student, you can verify this by grepping through the file "receipts.log",
which records all issued receipts.

Manually Generating a Receipt
-----------------------------

You should never need to do this manually since the webapp does this for you. But just in case:

```bash
./generate.sh data/key.bin start.jar
```

Future Features
===============

* Protection against bots
* Logging all receipts to a central file


References
==========

http://wiki.eclipse.org/Jetty/Reference/Dependencies
http://wiki.eclipse.org/Jetty/Feature/WebAppDeployer
http://www.enavigo.com/2008/08/29/deploying-a-web-application-to-jetty/
http://scalate.fusesource.org/documentation/user-guide.html