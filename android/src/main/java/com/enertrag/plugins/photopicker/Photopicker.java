package com.enertrag.plugins.photopicker;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.getcapacitor.FileUtils;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

@NativePlugin(
        requestCodes = {
                Photopicker.REQUEST_IMAGE_PICK
        },
        permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
        }
)
public class Photopicker extends Plugin {

    protected static final int REQUEST_IMAGE_PICK = 1603;

    @PluginMethod
    public void getPhotos(PluginCall call) {

        if(hasRequiredPermissions()) {
            _getPhotos(call);
        } else {
            Log.d("DEBUG LOG", "permission required");
            saveCall(call);
            pluginRequestAllPermissions();
        }

    }

    private void _getPhotos(PluginCall call) {

        Log.i("DEBUG LOG", "_getPhotos() called");

        saveCall(call);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        //intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(call, intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        PluginCall savedCall = getSavedCall();

        if(savedCall == null) {
            return;
        }

        for(int result: grantResults) {
            if(result == PackageManager.PERMISSION_DENIED) {
                savedCall.error("User denied permission");
                return;
            }
        }

        _getPhotos(savedCall);
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);

        PluginCall savedCall = getSavedCall();

        if(savedCall == null) {
            return;
        }

        JSObject result = new JSObject();

        JSArray urls = new JSArray();
        result.put("urls", urls);

        if(requestCode == REQUEST_IMAGE_PICK) {

            if(resultCode == RESULT_OK) {

                if(data.getClipData() != null) {

                    int count = data.getClipData().getItemCount();
                    for(int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        urls.put(FileUtils.getFileUrlForUri(getContext(), imageUri));
                    }

                } else if(data.getData() != null) {
                    Uri imageUri = data.getData();
                    urls.put(FileUtils.getFileUrlForUri(getContext(), imageUri));
                }

                result.put("selected", true);

                savedCall.resolve(result);
            } else if(resultCode == RESULT_CANCELED) {

                result.put("selected", false);
                savedCall.resolve(result);
            }
        }
    }
}
