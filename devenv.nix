{ pkgs, config, ... }:
{
  packages = [ pkgs.git pkgs.curl pkgs.python3 pkgs.unzip ];
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk21;
  };
  android = {
    enable = true;
    # android-nixpkgs uses a dash for the SDK's Android 37.0 package.
    platforms.version = [ "37-0" ];
    buildTools.version = [ "36.0.0" ];
    emulator.enable = false;
    systemImages.enable = false;
    ndk.enable = true;
    ndk.version = [ "28.2.13676358" ];
    cmake.version = [];
    extras = [];
  };
  scripts = {
    compile.exec = ''
      cd "${config.devenv.root}"
      ./gradlew :app:assembleDebug "$@"
    '';
    lint.exec = ''
      cd "${config.devenv.root}"
      ./gradlew :app:lintDebug "$@"
    '';
    test.exec = ''
      cd "${config.devenv.root}"
      ./gradlew :app:testDebugUnitTest "$@"
    '';
    test-device.exec = ''
      cd "${config.devenv.root}"
      ./gradlew :app:connectedDebugAndroidTest "$@"
    '';
    upload-app.exec = ''
      cd "${config.devenv.root}"
      python3 scripts/upload-app.py "$@"
    '';
    fetch-model.exec = ''
      cd "${config.devenv.root}"
      python3 scripts/fetch-model.py
    '';
  };
}
