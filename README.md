# A simple auto-updater for Eclipse RCP applications

One of the most common requests I get from my consulting clients is for an easy way to auto-update deployed Eclipse RCP applications. Particularly in enterprise environments these clients have many deployed installations and often require different feature sets deployed to different user groups.

I’ve found that for most of my clients a very simple auto-updater works well. It does two things.

1. Compares the versions of installed features with those on a p2 repository and update features with newer versions.
2. Creates a list of repository features that are not installed locally, and allow those features to be installed.

Because I get asked about this quite a bit, I’ve decided to create a version of the auto-updater available under the Eclipse Public License.

## Installing the auto-updater

While you can download the auto-updater source as well as the example usage projects, you can also simply add the feature to your product using this p2 repository.

https://github.com/modular-mind/updatemanager/raw/master/repository/

When you run your application, the `UpdateManager` service will be deployed using Declarative Services and can be accessed as other OSGi services can.

## Using the auto-updater in the Application class

For RCP applications using the compatibility layer, auto-updates are often performed in the `Application.start()` method. In this case, the service needs to be retrieved somehow and in the example code I’m using a `ServiceTracker` in the plug-in `Activator`.

```java
@Override
public Object start(IApplicationContext context) throws Exception {
    Display display = PlatformUI.createDisplay();
    try {
        UpdateManager updateManager = Activator.getDefault().getUpdateManager();
        if (updateManager.performAutoUpdate()) {
            return IApplication.EXIT_RESTART;
        }
         
        int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
        if (returnCode == PlatformUI.RETURN_RESTART)
            return IApplication.EXIT_RESTART;
        else
            return IApplication.EXIT_OK;
    } finally {
        display.dispose();
    }
}
```

## Using the auto-updater in a splash handler

Another common location to put auto-update logic is in a splash handler. Again, you would need to access the service through a `ServiceTracker` or the equivalent.

For applications using the compatibility layer, I’ve found a good pattern is to store whether an update occurred in a preference setting and then do the actual restart in the `ApplicationWorkbenchAdvisor` class.

## Using the auto-updater in a lifecycle manager

It’s also possible to use the auto-updater in a lifecycle handler class. The call should be made in the method with an `@PostContextCreate` annotation. Unfortunately it’s not possible to access the workbench in this method to restart the application, so [you’ll need to add a listener to do that work](https://stackoverflow.com/questions/23342341/restart-eclipse-4-rcp-application-before-it-gets-visible). Also, in this case you’ll see the workbench window briefly appear before the restart.

On a side note, Tom Schindl has done some work for the e(fx)clipse project that [provides restart capability from the lifecycle handler](https://tomsondev.bestsolution.at/2014/11/03/efxclipse-1-1-new-features-api-to-restart-your-e4-app-on-startup/). I’ve added a [Bugzilla entry](https://bugs.eclipse.org/bugs/show_bug.cgi?id=571412) to see if we can reuse this logic more generally.

## Customizing the auto-updater

Currently there a few ways to customize the behavior of the auto-updater through Declarative Services. You can provide any of the following services to integrate with the `UpdateManager`.

* `UpdateManagerRepositoryLocator` – Can return a string representing the URI for the remote p2 repository. This allows you to calculate the URI based on factors such as environment (dev, prod, etc.) or other factors. If no locator is provided, the auto-updater looks for a repository system argument passed on start-up.
* `UpdateManagerInstallFilter` – Each feature that is discovered in the remote repository and that is not currently installed will be passed to this filter. You can decide which features to install and the default is to allow the install. One use-case for this is to look up a users privileges to decide whether they can access a particular feature.
* `UpdateManagerLogger` – Allows you to hook up whatever logging framework you like. Default is to call sys out.
