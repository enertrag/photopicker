# Capacitor (Multi-) Photopicker Plugin

tbd; description needed

## Installation

```bash
npm install enertrag-photopicker
```

_(Of course, the usual Capacitor procedure <code>npx cap sync</code> must be executed afterwards.)_

## iOS Notes

**_Important:_** **this plugin requires iOS 14 or later.**

Selected images are persisted in the users documents folder. As mentioned you should move it to the final destination.

## Android Notes

To use this plugin you have to register it in your MainActivity.

```java
import ...
import com.enertrag.plugins.photopicker.Photopicker;

public class MainActivity extends BridgeActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initializes the Bridge
    this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
      // Additional plugins you've installed go here
      add(Photopicker.class);
    }});
  }
}
```

On your MainActivity.java file add <code>import com.enertrag.plugins.photopicker.Photopicker;</code> and then inside the init callback <code>add(Photopicker.class);</code>

## Example

```typescript
import { Plugins } from '@capacitor/core';
const { Photopicker } = Plugins;

...

async addPhotos() {

      const result = await Photopicker.getPhotos();
      if (result.selected) {

        for (const url of result.urls) {
            // ... do something with the url
        }
      }
}
```

Alternatively, if the code completion does not work, the import can be formulated as follows:

```typescript
import { EAGPhotopicker } from 'enertrag-photopicker';
const Photopicker = new EAGPhotopicker();
```

## API

### getPhotos()

```typescript
getPhotos(options: PhotopickerOptions) => Promise<PhotopickerResponse>
```

Prompt the user to pick one or more photos from an album.

The method call may fail if the user denies the permission request.

**Returns:** <code>Promise&lt;PhotopickerResponse&gt;</code>

## Interfaces

### PhotopickerOptions

| Prop          | Type                 | Description |
| ------------- | -------------------- | ----------- |
| **`maxSize`** | <code>number?</code> |             |
| **`quality`** | <code>number</code>  |             |

### PhotopickerResponse

| Prop           | Type                  | Description |
| -------------- | --------------------- | ----------- |
| **`selected`** | <code>boolean</code>  |             |
| **`urls`**     | <code>string[]</code> |             |

## Implementation

The exciting parts of the source code for Android can be found [here](https://github.com/enertrag/photopicker/blob/main/android/src/main/java/com/enertrag/plugins/photopicker/Photopicker.java). The ones for iOS are [here](https://github.com/enertrag/photopicker/blob/main/ios/Plugin/Plugin.swift).

## License

[MIT](https://github.com/enertrag/photopicker/blob/main/LICENSE)

Copyright © 2021 Philipp Anné
