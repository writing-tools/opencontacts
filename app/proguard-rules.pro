# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/sultanm/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# ez-vcard
-dontwarn ezvcard.io.json.**            # JSON serializer (for jCards) not used
-dontwarn freemarker.**                 # freemarker templating library (for creating hCards) not used
-dontwarn org.jsoup.**                  # jsoup library (for hCard parsing) not used
-dontwarn sun.misc.Perf
-keep,includedescriptorclasses class ezvcard.property.** { *; } # keep all VCard properties (created at runtime)

# for sugardb to work
# Ensures entities remain un-obfuscated so table and columns are named correctly
-keep class opencontacts.open.com.opencontacts.orm.** { *; }

#lodash
-dontwarn com.github.underscore.lodash.*