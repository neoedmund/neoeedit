neoeedit
========

neoeedit - a smart and light GUI text editor

<pre>

https://github.com/neoedmund/neoeedit



Neoeedit is a light, quick, smart, simple GUI text editor mainly written in Java. Size is only about 100KB(Jar File).

The target is to be used daily by everyone specially programmers.

Current status: Stable.

Newest Version of Java(JRE) need to be installed.

Download (dist/neoeedit.jar)

plugins (Pinyin/Japanese IME)

You can use it in any of the methods:




features:

    basic functions of text editor
    small footprint (about 100KB binary size)
    quick show (customized Swing component)
    good unicode support
    run where Java run: Windows, Linux, ...
    IME-Aware, on-the-spot pre-edting.

    Fonts, colors and all the following shortcut keys can be configured by editing config file(user's-home-dir/.neoeedit/data.py.verX)
    
    ctrl-C/V/X for copy/paste/cut
    select text using both mouse and keyboard
    cursor keys: up down left right home end(also Ctrl-Enter) pageup pagedown
    alt-pageup, alt-pagedown, alt-mouse scroll: horizon cursor movement
    line number

    alt-Z: move cursor back by history
    alt-Y: move cursor forward by history
    ctrl-L: goto line
    ctrl-A: select all
    ctrl-D: delete current line
    ctrl-R: remove all trailing space
    alt-H:show hex for selected string
    alt-W: wrap lines.(at current X(min 10), non-English character's width calculated as two.)

    ctrl-S: save file
    F2 : save as...
    ctrl-O: open file in directory. It just list them, and use ctrl-G to open one of them.
    drag and drop files to open
    ctrl-N: new empty document in window
    ctrl-M: new one More window.
    ctrl-Q: show all opened documents in window. You can jump to one of them by press ctrl-1 over it.
    ctrl-1: go to file(:lineno) of current line.
    ctrl-Tab: quick switch between opened documents.
    ctrl-W: close current document, and record to open file history.
    ctrl-G or ctrl-1: goto file and line on search result or file by name or document in the window by name.
    Alt-L: launch current line using system default launcher(for file, executable, text, or URL).
    Alt-E: execute current line in system command line(eg. for windows, try "cmd /c dir").
    ctrl-H: open file history
    ctrl-P: print (beautifully)

    ctrl-Z: undo
    ctrl-Y: redo

    ctrl-F: find/replace
    F3 : find next

    ctrl-E: set encoding
    F5 : reload with encoding
    alt-S : change line seperator between windows(\r\n) and unix(\n)

    alt-left alt-right: quick indent
    home end, ctrl up,down,left,right: cursor control

    common language keywords highlighting (java,c,python,basic, 500+ words)

    (){}<> pair marking. Alt-P: move cursor between pair marks.

    F1: help(about)

    Encoding auto detection . for UTF-8, UTF-16, UTF-32, SJIS, GBK. Good unicode support.
    Comments auto detection . like # // -- for many languages.

    replace/find in files, and in dir/sub-dir

    alt-\: rectangular mode

    ctrl-mouse scroll: zoom in/out
    ctrl-0: zoom reset

    alt-f:list fonts, then ctrl-1 on it to change font

    alt-j: javascript macro

    alt-c: switch between preset color modes, there are 3 now: White, Black, Blue.

    It's also a image viewer! View large JPG, GIF, BMP, PNG images easily.
        Left, Right: view previous/next image
        Up, Down: rotate image
        0: reset image
        Ctrl-W / H / O: functions like what it did in text editor mode
        F1 or TAB: toggle thumbnail

    configurable custom Freetype font

    integrated NeoeIme as a plugin.(see Plugins)

        ctrl-space to toggle IME

    hold CTRL for a second: show command panel.
    (more... )

</pre>
