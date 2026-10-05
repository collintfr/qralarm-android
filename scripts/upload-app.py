"""Build and install without guessing which connected device to overwrite."""
import argparse
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("serial", nargs="?", help="adb device serial (required with multiple devices)")
args = parser.parse_args()
lines = subprocess.check_output(["adb", "devices"], text=True).splitlines()[1:]
devices = dict(line.split()[:2] for line in lines if len(line.split()) >= 2)
serial = args.serial
if serial is None:
    if len(devices) != 1:
        parser.error("Connect one Android device, or specify its serial. Run adb devices to inspect connections.")
    serial = next(iter(devices))
if devices.get(serial) != "device":
    parser.error(f"Device {serial} is missing, offline, or unauthorized. Enable USB debugging and authorize this computer.")
subprocess.run(["./gradlew", ":app:assembleDebug"], check=True)
subprocess.run(["adb", "-s", serial, "install", "-r", "app/build/outputs/apk/debug/app-debug.apk"], check=True)
