import { Plugins } from '@capacitor/core';
import { PhotopickerResponse, PhotopickerOptions } from './definitions';

const { Photopicker } = Plugins;

export class EAGPhotopicker {
    constructor() { }

    getPhotos(options: PhotopickerOptions): Promise<PhotopickerResponse> {
        return Photopicker.getPhotos(options);
    }
}
