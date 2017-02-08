#Bamana

[![Build Status](https://travis-ci.org/ebruni/bamana.svg?branch=master)](https://travis-ci.org/ebruni/bamana)

**Bamana is a free** (GNU GPLv3) **command line file based incremental backup software** for GNU/Linux written in Java, optimized for SSDs and multicore processors, sooner or later to be ported to Windows and MacOS.

The first snapshot of a newly initialized backup recursively saves the source directory into the backup's archive, while subsequent snapshots only save those files which have been added or modified since the previous snapshot. Deleted files and previous versions of modified files (including their metadata) are kept saved so that they can be restored when restoring a corresponding snapshot. Restoring a snapshot doesn't require to restore previous snapshots. Moving/renaming a file doesn't require to create a new copy of it.

###Features

#####Current
- Attributes preservation: group, owner, permissions, symbolic link target, creation time, last access time, last modified time
- Integrity checks
- Exclusion of selected subdirectories
- Duplicate files are stored only once
- Optimized for SSDs and multicore CPUs.

#####To be introduced ASAP
- Support for Windows filesystems
- Creation of backups with more than one root directory
- Data and metadata compression and encryption (metadata currently weights far more than it should)
- Remote backups
- File locking and checks for existing locks
- Symlink following
- More optimizations for SSDs and multicore CPUs
- Faster integrity checks (at the moment they can be really slow)
- UI and UX improvements
- GUI
- Much more.
Suggestions are warmly welcome.

###Installation guide
Binaries are not available yet, anyway compiling from source is extremely easy. Just be sure to have git and either OpenJDK 7+ or Oracle JDK 7+ in your system before running the installer.

```
git clone https://github.com/ebruni/bamana
cd bamana
sudo ./SETUP
```
and follow the instructions.

###Usage
Step 1. Initialize a backup for the directory you want to backup. `cd` to the directory and type:  
`bmn init -i`

Step 2. Take a snapshot of that directory. `cd` to the directory and type:  
`(sudo) bmn snap -i`  

Step 3. Restore a snapshot. `cd` to the directory where you want to restore the snapshot and type:  
`(sudo) bmn restore -i`

Using sudo is advised to avoid missing permissions related issues.

**Need help** (or just wanna have a chat)**?** Join Bamana's telegram group: https://t.me/joinchat/AAAAAD6fZmMrP9lcxZsS5g

###Current version
0.0.1

###Changelog
*No significant changes yet.*

###Development status
I've done many snapshots and restores of directories containing tens of thousands of elements and everything seems to work as it should. Minor bugs and unhandled events can still pop out but nothing critical as far as I know.
I can't guarantee about the strenght of the hashing function at the moment (currently MD5, will likely switch to SHA-1 or higher in the near future), and there are no collision detection mechanisms yet.
Code and user interface are sometimes inelegant and logs are often vague and messy. I'm planning to do a gradual refactoring and to produce a decent documentation both for users and developers in the next months.  
Check the Issues section for the currently known/reported issues.

###FAQ

Q: Are you using Bamana for your backups?
A: Absolutely yes.

Q: Are newer versions backward compatible with backups created with older versions?  
A: As a general rule yes: legacy code will be kept active and bugfixed when necessary. I'll publish a detailed backward compatibility chart on this page if backward compatibility issues will ever arise.
