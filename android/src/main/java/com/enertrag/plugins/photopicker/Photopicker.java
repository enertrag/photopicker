// Copyright © 2021 Philipp Anné
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this
// software and associated documentation files (the “Software”), to deal in the Software
// without restriction, including without limitation the rights to use, copy, modify, merge,
// publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
// to whom the Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
// PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
// FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.

package com.enertrag.plugins.photopicker;

import android.Manifest;
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
                Photopicker.REQUEST_IMAGE_PICK,
                Photopicker.REQUEST_READ_PERMISSION,
        },
        permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
        }
)
public class Photopicker extends Plugin {

    protected static final int REQUEST_IMAGE_PICK = 1603;
    protected static final int REQUEST_READ_PERMISSION = 1604;

    private static final String LOG_TAG = "ENERTRAG/Photopicker";

    /**
     * Implementation of the Plugin-API.
     * This method lets the user select one or more photos and returns their URIs
     *
     * @param call the Capacitor call data
     */
    @PluginMethod
    public void getPhotos(PluginCall call) {

        Log.v(LOG_TAG, "entering getPhotos()");

        if(hasRequiredPermissions()) {

            _getPhotos(call);

        } else {

            Log.d(LOG_TAG, "requesting all permissions");

            saveCall(call);

            // Fix: we must not use pluginRequestAllPermissions() here, because it
            //      won't call back the handleRequestPermissionResult() method.
            //      Maybe I misunderstood the documentation.
            pluginRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_PERMISSION);
        }

    }

    /**
     * Opens the Android activity for selecting photos.
     *
     * @param call the Capacitor call data
     */
    private void _getPhotos(PluginCall call) {

        Log.v(LOG_TAG, "entering _getPhotos()");

        saveCall(call);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        // This is the magic "extra" for selecting more than one photo
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        startActivityForResult(call, intent, REQUEST_IMAGE_PICK);
    }

    /**
     * Callback method that is executed after the user has answered the permission check.
     *
     * @param requestCode the code from the pluginRequestPermission(...) call
     * @param permissions the requested permissions
     * @param grantResults the user selection with regard to permissions
     */
    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Log.v(LOG_TAG, "entering handleRequestPermissionsResult()");

        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        PluginCall savedCall = getSavedCall();

        if(savedCall == null) {
            Log.d(LOG_TAG, "saved call data not found");
            return;
        }

        if(requestCode == REQUEST_READ_PERMISSION) {

            Log.d(LOG_TAG, "permission request was answered");

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {

                    Log.i(LOG_TAG, "user denied permission");

                    savedCall.error("User denied permission");
                    return;
                }
            }

            _getPhotos(savedCall);
        }
    }

    /**
     * Callback method that is executed after the user has selected his photos
     *
     * @param requestCode the code from the startActivityForResult(...) call
     * @param resultCode a code indicating whether the user has completed or cancelled the selection
     * @param data the activity specific data (see below)
     */
    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {

        Log.v(LOG_TAG, "entering handleOnActivityResult()");

        super.handleOnActivityResult(requestCode, resultCode, data);

        PluginCall savedCall = getSavedCall();

        if(savedCall == null) {
            Log.d(LOG_TAG, "saved call data not found");
            return;
        }

        JSObject result = new JSObject();

        JSArray urls = new JSArray();
        result.put("urls", urls);

        if(requestCode == REQUEST_IMAGE_PICK) {

            if(resultCode == RESULT_OK) {

                // Thanks to the internet for figuring this out :)
                if(data.getClipData() != null) {

                    Log.d(LOG_TAG, "user selected multiple photos");

                    int count = data.getClipData().getItemCount();
                    for(int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        urls.put(FileUtils.getFileUrlForUri(getContext(), imageUri));
                    }

                } else if(data.getData() != null) {

                    Log.d(LOG_TAG, "user selected single photo");

                    Uri imageUri = data.getData();
                    urls.put(FileUtils.getFileUrlForUri(getContext(), imageUri));
                }

                result.put("selected", true);

                savedCall.resolve(result);

            } else if(resultCode == RESULT_CANCELED) {

                Log.d(LOG_TAG, "user canceled selection");

                result.put("selected", false);
                savedCall.resolve(result);
            }
        }
    }
}
