# Background

The current bar code/QR code scanner is inconsistent and has to be aimed with care at the target label in order to shut off the alarm. Often, the app simply fails to read bar codes entirely. It would be ideal if, instead of using a QR code or bar code which requires precision detection, we could alternatively choose from a list of objects and have the alarm shut off for a particular object.

# Requirements

## Repository Layout

You must set up the `devenv.nix` for the repository, in order to install all requisite dependencies. You must also place all important commands and scripts as commands within `devenv.nix`. This would include compilation (e.g. `compile`), linting, and deploying the app (e.g. `upload-app`).   

## Object Recognition

The object recognition method should be an off the shelf, lightweight neural network. It should be tailored to classification, and among its classes should include common household items/locations such as toilet, toothbrush, water bottle, and sink. The user should be able to choose between the existing option of using a QR code or bar code, and should have an alternative option to turn on the object recognition feature. The user should be able to select a single object from the list of all objects the network can classify. 

When the camera is on, i.e. when the alarm is ringing, it should check every frame for whether it includes the object chosen by the user, and, if it is sufficiently confident over several frames, should resolve the alarm. The system should be optimized to reduce false positives and false negatives.   
