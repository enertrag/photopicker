
export interface PhotopickerResponse {
  selected: boolean;
  urls: string[];
}

export interface PhotopickerOptions {
  maxSize: number;
  quality: number;
}

export interface PhotopickerPlugin {

  /**
   * Prompt the user to pick one or more photos from an album.
   * 
   * @since 1.0
   */
  getPhotos(options: PhotopickerOptions): Promise<PhotopickerResponse>;
}
