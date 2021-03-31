declare module '@capacitor/core' {
  interface PluginRegistry {
    Photopicker: PhotopickerPlugin;
  }
}

export interface PhotopickerResponse {
  selected: boolean;
  urls: string[];
}

export interface PhotopickerPlugin {

  /**
   * Prompt the user to pick one or more photos from an album.
   * 
   * @since 1.0
   */
  getPhotos(): Promise<PhotopickerResponse>;
}
