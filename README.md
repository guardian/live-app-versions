# live-app-versions

### android-live-app-versions

Retrieves information about releases of the [Android Live App](https://github.com/guardian/android-news-app) from the [Play Developer API](https://developers.google.com/android-publisher#publishing) and stores the results in S3.

### ios-live-app-versions

Retrieves information about beta releases of the [iOS Live App](https://github.com/guardian/ios-live) from the 
[App Store Connect API](https://developer.apple.com/app-store-connect/api/) and stores the results in S3.

### ios-deployments

Distributes releases of the [iOS Live App](https://github.com/guardian/ios-live) to external beta testers and provides an audit trail of releases via [GitHub deployments](https://developer.github.com/v3/repos/deployments/#deployments).

## Infrastructure

The project is implemented in Scala as a collection of AWS Lambdas. The lambdas run on a regular schedule; results are stored in an S3 bucket which sits behind Fastly. 

The infrastructure for serving JSON via S3 and Fastly is shared with [`mobile-static`](https://github.com/guardian/mobile-static#infrastructure).

You can view the latest iOS beta builds via https://mobile.guardianapis.com/static/reserved-paths/ios-live-app/recent-beta-releases.json.

## Build

Note that this project now uses a github [workflow](.github/workflows/build.yml) to build the artefacts for deployment. The project name in riff raff is `mobile::live-app-versions`

## Testing in CODE

1. Enable the [rule](https://eu-west-1.console.aws.amazon.com/events/home?region=eu-west-1#/eventbus/default/rules/live-app-versions-CODE-PollingEvent-895CZLHBBP9C)
to poll the lambdas.
2. Click on the targets tab to find a link to each of the lambdas.
3. On the monitor tab on the lambda console, click view cloudwatch logs.
4. Check for a successful invocation of the lambda when the schedule runs.
5. Make sure to turn off the rule promptly after testing (do not let it run for too long) as the CODE lambda hits the same endpoint that android uses to release the app. We have a daily quota for using this endpoint and android app releases will be affected if exceeded.
