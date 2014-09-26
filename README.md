VideoViewCache
==============

Simple example proxy with range for standart mediaplayer.

Principle of operation:
Loading occurs in two threads. The first stream player. Second saves the file on disk.
If request exist in file, then player play file else from internet.
