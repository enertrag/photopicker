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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.getcapacitor.FileUtils;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import androidx.activity.result.ActivityResult;

@CapacitorPlugin(
        name = "Photopicker",
        permissions = {
                @Permission(strings = { Manifest.permission.READ_EXTERNAL_STORAGE }, alias = "READ_EXTERNAL_STORAGE"),
                @Permission(strings = { Manifest.permission.WRITE_EXTERNAL_STORAGE }, alias = "WRITE_EXTERNAL_STORAGE")
        }
)
public class Photopicker extends Plugin {

    private static final String LOG_TAG = "ENERTRAG/Photopicker";

    private PhotopickerOptions options;

    /**
     * Implementation of the Plugin-API.
     * This method lets the user select one or more photos and returns their URIs
     *
     * @param call the Capacitor call data
     */
    @PluginMethod
    public void getPhotos(PluginCall call) {

        Log.v(LOG_TAG, "entering getPhotos()");

        options = getOptions(call);
        if(!checkOptions(call)) {
            return;
        }

        if(allPermissionsGranted()) {

            _getPhotos(call);

        } else {
            Log.d(LOG_TAG, "requesting all permissions");

            requestPermissionForAliases(
                    new String[] {"READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"},
                    call,
                    "requestPermissionsCallback");
        }
    }

    @PermissionCallback
    private void requestPermissionsCallback(PluginCall call) {
        if (allPermissionsGranted()) {
            _getPhotos(call);
        } else {
            Log.i(LOG_TAG, "user denied permission");
            call.reject("User denied permission");
        }
    }

    private boolean allPermissionsGranted(){
        return getPermissionState("READ_EXTERNAL_STORAGE") == PermissionState.GRANTED &&
               getPermissionState("WRITE_EXTERNAL_STORAGE") == PermissionState.GRANTED;
    }

    /**
     * Extracts the photopicker options from the capacitor call.
     *
     * @param call the Capacitor call data
     * @return the photopicker call options
     */
    private PhotopickerOptions getOptions(PluginCall call) {

        Log.v(LOG_TAG, "entering getOptions()");

        PhotopickerOptions result = new PhotopickerOptions();

        result.setMaxSize(call.getInt("maxSize", PhotopickerOptions.DEFAULT_MAX_SIZE));
        result.setQuality(call.getInt("quality", PhotopickerOptions.DEFAULT_QUALITY));

        return result;
    }

    /**
     * Checks if all photopicker options are valid.
     *
     * @param call the Capacitor call data
     * @return true if all options are valid, false otherwise
     */
    private boolean checkOptions(PluginCall call) {

        Log.v(LOG_TAG, "entering checkOptions()");

        if(options.getMaxSize() < 0 || options.getMaxSize() > 10000) {

            Log.e(LOG_TAG, "invalid value for parameter maxSize (0-10000)");
            call.reject("invalid value for parameter maxSize (0-10000)");
            return false;
        }

        if(options.getQuality() < 1 || options.getQuality() > 100) {

            Log.e(LOG_TAG, "invalid value for parameter quality (10-100)");
            call.reject("invalid value for parameter quality (10-100)");
            return false;
        }

        return true;
    }


    /**
     * Opens the Android activity for selecting photos.
     *
     * @param call the Capacitor call data
     */
    private void _getPhotos(PluginCall call) {

        Log.v(LOG_TAG, "entering _getPhotos()");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        // This is the magic "extra" for selecting more than one photo
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        // startActivityForResult(call, intent, REQUEST_IMAGE_PICK);
        startActivityForResult(call, intent, "getPhotosResult");
    }

    @ActivityCallback
    private void getPhotosResult(PluginCall call, ActivityResult result) {
        Log.v(LOG_TAG, "entering getPhotosResult()");

        JSObject res = new JSObject();

        JSArray urls = new JSArray();
        res.put("urls", urls);

        if(result.getResultCode() == RESULT_OK) {

            Intent data = result.getData();

            // Thanks to the internet for figuring this out :)
            if(data.getClipData() != null) {

                Log.d(LOG_TAG, "user selected multiple photos");

                int count = data.getClipData().getItemCount();
                for(int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUri = getTempFile(imageUri);

                    urls.put(imageUri != null ? FileUtils.getFileUrlForUri(getContext(), imageUri) : null);                    }

            } else if(data.getData() != null) {

                Log.d(LOG_TAG, "user selected single photo");

                Uri imageUri = data.getData();
                imageUri = getTempFile(imageUri);

                urls.put(imageUri != null ? FileUtils.getFileUrlForUri(getContext(), imageUri) : null);
            }

            res.put("selected", true);

            call.resolve(res);

        } else if(result.getResultCode() == RESULT_CANCELED) {

            Log.d(LOG_TAG, "user canceled selection");

            res.put("selected", false);
            call.resolve(res);
        }
    }


    /**
     * Creates a temporary jepg image from the selected native mediapicker image uri.
     *
     * @param uri the selected image from the native mediapicker.
     * @return a temporary uri for a new created jepg image
     */
    private Uri getTempFile(Uri uri) {

        Log.v(LOG_TAG, "entering getTempFile()");

        InputStream imageStream = null;

        try {
            imageStream = getContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

            if (bitmap == null) {

                Log.e(LOG_TAG, "bitmap could not be decoded from stream");
                return null;
            }

            if(options.getMaxSize() > 0) {
                bitmap = resizeBitmapPreservingAspectRatio(bitmap, options.getMaxSize());
            }

            // Compress the final image and prepare for output to client
            ByteArrayOutputStream bitmapOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, options.getQuality(), bitmapOutputStream);

            return getTempImage(bitmapOutputStream);

        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "file not found", e);
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "imageStream could not be closed", e);
                }
            }
        }

        return null;
    }

    /**
     * Persists bitmap data to a temporary file and returns the uri.
     *
     * @param bitmapOutputStream the bitmap data as an output stream
     * @return the uri for the created jepg file
     */
    private Uri getTempImage(ByteArrayOutputStream bitmapOutputStream) {

        Log.v(LOG_TAG, "entering getTempImage()");

        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(bitmapOutputStream.toByteArray());

            String filename = "py_temp_" + UUID.randomUUID().toString() + ".jpeg";

            File cacheDir = getContext().getCacheDir();
            File outFile = new File(cacheDir, filename);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();

            return Uri.fromFile(outFile);

        } catch (IOException ex) {

            // Something went terribly wrong
            Log.e(LOG_TAG, "error writing temp file", ex);


        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "inputStream could not be closed", e);
                }
            }
        }

        return null;
    }

    /**
     * Resize an image to the given max width or height.
     * The maximum size always refers to the longer of the two sides.
     *
     * Resize will always preserve aspect ratio.
     *
     * @param bitmap the bitmap to resize
     * @param maxSize the new maximum side length
     * @return a new, scaled Bitmap
     */
    private static Bitmap resizeBitmapPreservingAspectRatio(Bitmap bitmap, final int maxSize) {

        Log.v(LOG_TAG, "entering resizeBitmapPreservingAspectRatio()");

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float newHeight;
        float newWidth;

        // resize with preserved aspect ratio
        if(width > height) {
            newWidth = Math.min(width, maxSize);
            newHeight = (height * newWidth) / width;

        } else {
            newHeight = Math.min(height, maxSize);
            newWidth = (width * newHeight) / height;
        }

        Log.i(LOG_TAG, "resizing bitmap from " + width + "x" + height + " to " + Math.round(newWidth)
        + "x" + Math.round(newHeight));

        return Bitmap.createScaledBitmap(bitmap, Math.round(newWidth), Math.round(newHeight), false);
    }



}
