# live-app-versions

Retrieves information about beta releases of the [iOS Live App](https://github.com/guardian/ios-live) from the 
[App Store Connect API](https://developer.apple.com/app-store-connect/api/) and stores the results in S3.

## Example Response

Note that this response only ever includes beta builds which have been released to external testers:

```
{
   "latestReleasedBuild" : {
     "version" : "17672"
   },
   "previouslyReleasedBuilds" : [
     {
       "version" : "17667"
     },
     {
       "version" : "17661"
     },
     {
       "version" : "17657"
     }
   ]
 }
```

## Infrastructure

The project is implemented in Scala as an AWS lambda. It runs on a regular schedule and stores results in an S3 bucket which sits behind Fastly. 

The infrastructure for serving JSON via S3 and Fastly is shared with [`mobile-static`](https://github.com/guardian/mobile-static#infrastructure).

You can view the latest beta builds via https://mobile.guardianapis.com/static/reserved-paths/ios-live-app/recent-beta-releases.json.