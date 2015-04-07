# StatTrackerApp
Herons Stats Screenshot Uploader

Permissions required and why:
  -  Access the Internet
    - We're uploading a file to the internet...
  -  Read external media files
    - Ingress saves the screenshot to a file, then passes us a reference (the file name)
      instead of passing the file itself. This means we have to go look it up and it's
      saved in the "external" (SD card, if present) file system.
