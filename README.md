# OmniEditor

*Ready-made JavaFX IDE for custom languages*

## Introduction

OmniEditor is a small JavaFX library providing a ready-made, user-friendly IDE for custom programming languages.

In addition to the basic document lifecycle activities (*new / open / save / save as / undo / redo / cut / copy / paste*), OmniEditor supports the execution of interpreters/compilers on the code written in the text editor: such a task can also be canceled by the user via the *Stop* button in the user interface (for example, in case of infinite loops).


## Core classes

### OmniEditor

The *OmniEditor* class provides a customizable IDE application packaged as a Java class.

To run it, just add a few lines to the *start* method of your JavaFX *Application* subclass:

```java
@Override
public void start(Stage primaryStage) {
  OmniEditor omniEditor = new OmniEditor(appStrategy);
  omniEditor.start(primaryStage);
}
```

where *appStrategy* is an instance of a class implementing the *AppStrategy* interface defined by the library, and *primaryStage* is the parameter of the *start* method.

For detailed information, please refer to the Javadoc documentation.


### StyledCodeEditor

Stemming from the "JavaKeywords" example provided by [RichTextFX](https://github.com/TomasMikula/RichTextFX), this class is the core of OmniEditor and creates a code editor easily customizable via CSS.

In particular:

* you can define a chain of *regex patterns*, each describing which CSS class should be applied to its matching text
* a shortcut method is defined to apply a CSS class to a set of tokens (especially keywords)
* it supports *smart new lines*, to keep each new line aligned with the previous one whenever the user presses ENTER
* it supports *dynamic tabs* - the insertion of a given number of space characters in lieu of \\t whenever the user presses TAB.


## Requirements

OmniEditor requires Java 8 Update 51 or later compatible version.


## Download

For further information about downloading or referencing OmniEditor via Gradle or Maven, please visit [its page](https://bintray.com/giancosta86/Hephaestus/OmniEditor) on Hephaestus, my Gradle repository.

## OmniEditor in action

The very first open source application employing OmniEditor is [Chronos IDE](https://github.com/giancosta86/Chronos-IDE), for the [Chronos programming language](https://github.com/giancosta86/Chronos).

## Special thanks

* OmniEditor employs [RichTextFX](https://github.com/TomasMikula/RichTextFX), and was created starting from its brilliant "JavaKeywords" demo

* The UI icons are taken from the elegant [Kids Icons](http://www.iconarchive.com/show/kids-icons-icons-by-everaldo.1.html) set, by [Everaldo Coelho](http://www.everaldo.com/).



### RichTextFX - License information

>Copyright (c) 2013-2014, Tomas Mikula
>All rights reserved.
>
>Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
>
>1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
>
>2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
>
>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
