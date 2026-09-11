# The app is pure Kotlin + Compose with no reflection, so R8's defaults plus the
# rules shipped by AndroidX are enough. Keep the line numbers, though: a stack
# trace from a Play crash report is unreadable without them.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
