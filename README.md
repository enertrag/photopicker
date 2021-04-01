# Capacitor (Multi-) Photopicker Plugin

tbd; description needed

## iOS Notes

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

## API

### getPhotos()

```typescript
getPhotos() => Promise<PhotopickerResponse>
```

Prompt the user to pick one or more photos from an album.

The method call may fail if the user denies the permission request.

**Returns:** <code>Promise&lt;PhotopickerResponse&gt;</code>

## Interfaces

### PhotopickerResponse

| Prop           | Type                  | Description |
| -------------- | --------------------- | ----------- |
| **`selected`** | <code>boolean</code>  |             |
| **`urls`**     | <code>string[]</code> |             |

## Implementation

The exciting parts of the source code for Android can be found [here](https://github.com/enertrag/photopicker/blob/main/android/src/main/java/com/enertrag/plugins/photopicker/Photopicker.java). The ones for iOS are here.

## License

MIT
