# djvu-html5
DjVu file viewer working as pure HTML5. Browse DjVu files without any additional tools or plugins!

Based on the DjVu viewer implementation for Java by LizardTech, Inc.
http://sourceforge.net/projects/javadjvu/

Adapted and optimized for [GWT framework](http://www.gwtproject.org/) by Mateusz Matela.

Released under the GNU General Public License version 2, see the [LICENSE](LICENSE) file for details.

## Live demo

**[Click here to open the viewer with a sample DjVu file](http://mateusz-matela.github.io/djvu-html5/demo.html)**

## Getting started

[Download](https://github.com/mateusz-matela/djvu-html5/releases) and unpack the latest version. Open the `Djvu_html5.html` file in a web browser to see the viewer with a sample document opened (this works best in Firefox, Chrome needs to be started with parameter `--allow-file-access-from-files` for this to work).
This html file can be used directly or as an example of how to invoke the viewer: reference the required stylesheet and javascript and add a `<div id="djvuContainer">` element that the viewer can attach to.
For best experience on mobile devices, it's recommended to **disable page scaling** with proper `<meta name="viewport" ...>` tag - otherwise a user may end up with resized view, not able to restore it as all touch input is intercepted by the djvu canvas.

The location of the DjVu document that should be opened can be defined in one of three ways:

* An attribute for the `div` element:`file="document.djvu"`
* A parameter in the URL: `?file=document.djvu`
* A property in a DJVU_CONTEXT object created in JavaScript before the page loading finishes:
```javascript
	var DJVU_CONTEXT = {
		file: "document.djvu"
	};
```
Other settings that can be defined in the `DJVU_CONTEXT` object are listed [here](https://github.com/mateusz-matela/djvu-html5/wiki/Advanced-configuration).

It is highly recommended to use Google Chrome - this browser is currently the fastest with the viewer.

**Note: the HTML file, the referenced JavaScript file and the DjVu document must all be located on the same host, otherwise the viewer will not work.**

## Integration with dLibra Digital Libraries Framework

[See instructions](extras/dlibra/README.md)

## Getting involved

The project is prepared with Eclipse and Google Plugin. If you want another tool stack, you're on your own (help with improving this welcome).

1. Dwonload Eclipse from https://www.eclipse.org/downloads/, recommended package is IDE for Jave EE Developers.
2. Dwonload GWT SDK 2.7.0 from http://www.gwtproject.org/download.html
3. In Eclipse, go to `Help` -> `Install New Software...`, enter update site https://dl.google.com/eclipse/plugin/4.4 and select Google Plugin for Eclipse (version for 4.4 works with Eclipse 4.5). Install and restart.
4. In `Preferences` -> `Google` -> `Web Toolkit` provide the location where GWT SDK was unpacked.
5. Clone the GIT repository and import project `djvu-html5`.
6. There's an error in the project: The GWT SDK JAR gwt-servlet.jar is missing in the WEB-INF/lib directory.
In the Markers view, use a quick fix `The GWT SDK JAR gwt-servlet.jar is missing in the WEB-INF/lib directory`.
7. Right-click on the project, `Run as` -> `Web Application (GWT Super Dev Mode)`. The viewer should now be available here: http://127.0.0.1:8888/Djvu_html5.html
8. To prepare distribution that can be put on an external web server, right-click on the project, `Google` -> `GWT Compile`. Select the project and proceed with the compilation. Copy `djvu-html5/war` to the external server.


## Maven project

The project can be added on Eclipse or IntelliJ. To compile from command line, the instructions are:
1. On Maven 
2. mvn clean install
3. mvn gwt:run
4. GWT Development Mode is opened, then we wait to the html is loaded.
5. When the terminal shows the message "The code server is ready at http://127.0.0.1:9876", the viewer should be available here: http://127.0.0.1:8888/Djvu_html5.html


