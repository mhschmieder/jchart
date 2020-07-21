# ChartToolkit
The ChartToolkit library is an open source project for Java 8 that serves as a small collection of basic utilities and custom classes for AWT and Swing based charting, covering some specific chart types that aren't often found in the better-known charting packages, or which simpler have an easier workflow an integration path when one doesn't need a mega-package to deal with one's charting needs.

At the moment, this is a bare bones skeletal utility library with only one method that doesn't even require AWT or Swing, just to decouple this from unrelated Graphics and Gui functionality in my other toolkits, and to make this method immediately available to anyone who needs it (it is a data reduction algorithm).

I may move that method to a math library later on, as it doesn't use anything outside the Java Core Language and as it is completely agnostic towards the charting package that will receive the data. For now, having this one function in this skeletal library serves another purpose of making this library the safest one for experimenting and learning about Maven builds and packaging, as I have been Subversion and Ant based until recently.

I plan to pull in a lot of legacy functions that have fallen out of use once I switched to mostly JavaFX based development, and improve them to be more up-to-date with Java 8 standards, but I do not have the ability at the moment to rigorously retest all of them (just the ones that find use where FX Charts is weak).

The initial release of Version 1 will require Java 8 as the JDK/JRE target, due to its use of newer language features. I will check to see if it might be made less restrictive as it may only need Java 7, but anything earlier than that introduces unpleasantires on macOS in terms of independent standalone builds.

There will be a modularized version soon, that supports Java 14+. If I find a way to make it compatible with other still-supported versions of Java (I think Java 11 is still supported by Oracle, but not Java 12 or Java 10, and maybe not Java 9 either), I will do what I can, but may have trouble finding an appropriate JDK still available to download and test against.

Maven support will hopefully be added within the next day or two.
