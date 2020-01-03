package io.flutter.plugins.camera;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import java.util.HashMap;
import java.util.Map;

class DartMessenger {
  @Nullable private EventSinkWrapper eventSink;

  enum EventType {
    ERROR,
    CAMERA_CLOSING,
    BARCODE,
  }

  DartMessenger(BinaryMessenger messenger, long eventChannelId) {
    new EventChannel(messenger, "flutter.io/cameraPlugin/cameraEvents" + eventChannelId)
        .setStreamHandler(
            new EventChannel.StreamHandler() {
              @Override
              public void onListen(Object arguments, EventChannel.EventSink sink) {
                eventSink = new EventSinkWrapper(sink);
              }

              @Override
              public void onCancel(Object arguments) {
                eventSink = null;
              }
            });
  }

  void sendCameraClosingEvent() {
    send(EventType.CAMERA_CLOSING, null);
  }

  void sendBarcodeEvent(String barcode) {
    send(EventType.BARCODE, barcode, null);
  }

  void send(EventType eventType, @Nullable String description) {
    send(eventType, null, description);
  }

  void send(EventType eventType, @Nullable String data, @Nullable String description) {
    if (eventSink == null) {
      return;
    }

    Map<String, String> event = new HashMap<>();
    event.put("eventType", eventType.toString().toLowerCase());
    if (!TextUtils.isEmpty(data)) {
      event.put("data", data);
    }
    // Only errors have a description.
    if (eventType == EventType.ERROR && !TextUtils.isEmpty(description)) {
      event.put("errorDescription", description);
    }
    eventSink.success(event);
  }

  // MethodChannel.EventSink wrapper that responds on the platform thread.
  private static class EventSinkWrapper implements EventChannel.EventSink {
    private EventChannel.EventSink eventSink;
    private Handler handler;

    EventSinkWrapper(EventChannel.EventSink sink) {
      eventSink = sink;
      handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void success(final Object result) {
      handler.post(() -> eventSink.success(result));
    }

    @Override
    public void error(final String errorCode, final String errorMessage, final Object errorDetails) {
      handler.post(() -> eventSink.error(errorCode, errorMessage, errorDetails));
    }

    @Override public void endOfStream() {
      handler.post(() -> eventSink.endOfStream());
    }
  }
}
