# Add project specific ProGuard rules here.
# Keep ONNX Runtime classes
-keep class com.microsoft.onnxruntime.** { *; }
-keepclassmembers class com.microsoft.onnxruntime.** { *; }
