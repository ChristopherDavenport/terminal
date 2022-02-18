node --enable-source-maps ./core/js/target/scala-2.13/terminal-fastopt/main.js
# This file exists because sbt is not a TTY so if run from SBT the feature that
# node uses for this don't exist. We may need a different way to represent this since
# sbt is such a common intermediate target.